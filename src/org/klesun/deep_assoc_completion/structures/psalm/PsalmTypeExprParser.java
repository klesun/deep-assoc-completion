package org.klesun.deep_assoc_completion.structures.psalm;

import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.lang.L;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.klesun.lang.Lang.*;

/**
 * see https://psalm.dev/docs/docblock_type_syntax/#array-types
 *
 * parses PSALM-format phpdoc tag string, example:
 *
 * @var \Generator<array{
 *     itemNo:string,
 *     variants:array<array{
 *         Code:string,
 *         stock:array<array{
 *                 serialNo:string,
 *                 locationCode:string,
 *                 differentialTaxation:bool
 *             }>
 *         }>
 *     }>
 * $products
 */
public class PsalmTypeExprParser
{
    final private String text;
    private int offset = 0;
    private L<String> lastMatch = new L<>();

    private PsalmTypeExprParser(String text)
    {
        this.text = text;
    }

    private String getTextLeft()
    {
        return Tls.substr(this.text, this.offset);
    }

    private boolean unprefix(String regex)
    {
        // for simplicity sake, taking a substring for now, but perfectly would
        // be to work with full string and pass offset to the regex function
        String textLeft = this.getTextLeft();
        Opt<L<String>> matchOpt = Tls.regexWithFull(regex + "(.*)", textLeft, Pattern.DOTALL);
        if (!matchOpt.has()) {
            return false;
        } else {
            L<String> match = matchOpt.unw();
            // damn java does not allow to match without implicit $ in end of pattern
            String textLeftNow = match.lst().unw();
            int shift = textLeft.length() - textLeftNow.length();
            if (shift < 1) {
                // could lead to an infinite loop if you have a mistake in some of regexes
                throw new RuntimeException("Empty pattern match in PSALM parser - /" + regex + "/ on text - " + Tls.substr(textLeft, 0, 20));
            }
            this.offset += shift;
            this.lastMatch = match.sub(0, -1);
            return true;
        }
    }

    private Opt<List<IType>> parseTypeList()
    {
        ArrayList<IType> types = new ArrayList<>();
        do {
            Opt<? extends IType> typeOpt = parseMultiValue();
            if (typeOpt.has()) {
                types.add(typeOpt.unw());
            } else {
                return non();
            }
        } while (this.unprefix("\\s*,\\s*"));
        return som(types);
    }

    private Opt<String> parseString(char quote)
    {
        boolean escape = false;
        StringBuilder result = new StringBuilder();
        for (; this.offset < this.text.length(); ++this.offset) {
            char ch = this.text.charAt(this.offset);
            if (escape) {
                result.append(ch);
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else if (ch == quote) {
                ++this.offset;
                return som(result.toString());
            } else {
                result.append(ch);
            }
        }
        return non();
    }

    private String skipTillClosed(char openCh, char closeCh)
    {
        int level = 1;
        int start = this.offset;
        for (; this.offset < this.text.length(); ++this.offset) {
            char ch = this.text.charAt(this.offset);
            if (ch == openCh) {
                ++level;
            } else if (ch == closeCh) {
                --level;
                if (level <= 0) {
                    String closed = Tls.substr(this.text, start, this.offset);
                    ++this.offset;
                    return closed;
                }
            } else if (ch == '"' || ch == '\'') {
                parseString(ch);
            }
        }
        String closed = Tls.substr(this.text, start);
        this.offset = this.text.length();
        return closed;
    }

    private Opt<TAssoc> parseAssocKeys()
    {
        LinkedHashMap<String, IType> keys = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> keyToComments = new LinkedHashMap<>();
        while (this.unprefix("\\s*(\\w+)\\s*:\\s*")) {
            String keyName = this.lastMatch.get(1);
            Opt<? extends IType> typeOpt = parseMultiValue();
            if (typeOpt.has()) {
                keys.put(keyName, typeOpt.unw());
                if (!unprefix("\\s*\\,\\s*")) {
                    break; // end of array, since no coma
                }
                while (this.unprefix("\\s*\\/\\/\\s*(.*?)\\s*\\n\\s*")) {
                    String comment = lastMatch.get(1);
                    if (!keyToComments.containsKey(keyName)) {
                        keyToComments.put(keyName, new ArrayList<>());
                    }
                    keyToComments.get(keyName).add(comment);
                }
            } else {
                break;
            }
        }
        unprefix(",\\s*"); // optional trailing coma
        if (unprefix("\\s*}\\s*")) {
            return som(new TAssoc(keys, keyToComments, ""));
        } else {
            String unparsed = skipTillClosed('{', '}');
            return som(new TAssoc(keys, keyToComments, unparsed));
        }
    }

    private Opt<TFunc> parseFunc()
    {
        ArrayList<TFunc.Param> params = new ArrayList<>();
        do {
            Opt<? extends IType> typeOpt = parseMultiValue();
            if (!typeOpt.has()) {
                System.out.println("no arg type opt huj");
                return non();
            }
            IType type = typeOpt.unw();

            boolean isOptional = this.unprefix("\\=");
            boolean isSpread = this.unprefix("\\.\\.\\.");

            params.add(new TFunc.Param(type, isOptional, isSpread));
        } while (this.unprefix("\\s*,\\s*"));

        if (!this.unprefix("\\s*\\)\\s*:\\s*")) {
            System.out.println("no ending after " + params.size());
            return non();
        }
        return parseMultiValue()
            .map(returnType -> new TFunc(params, returnType));
    }

    private Opt<? extends IType> parseSingleValue()
    {
        this.unprefix("\\s+");
        // a comment, just ignore for now
        this.unprefix("\\/\\/.*\\n");
        Opt<? extends IType> parsed = non();
        if (this.unprefix("([a-zA-Z\\\\_][a-zA-Z\\\\_0-9\\-]*)\\s*<\\s*")) {
            String fqn = this.lastMatch.get(1);
            parsed = parseTypeList()
                .map(generics -> new TClass(fqn, generics))
                .flt(t -> unprefix("\\s*>\\s*"));
        } else if (this.unprefix("([a-zA-Z\\\\_][a-zA-Z\\\\_0-9\\-]*)\\s*((?:\\[\\])+)\\s*")) {
            // traditional phpdoc format for array of type
            String fqn = this.lastMatch.get(1);
            String bracketsStr = this.lastMatch.get(2);
            IType wrappedType = new TClass(fqn, L.non());
            for (int i = 0; i < bracketsStr.length() / 2; ++i) {
                wrappedType = new TClass("array", list(wrappedType));
            }
            parsed = som(wrappedType);
        } else if (this.unprefix("array\\s*\\{\\s*")) {
            parsed = parseAssocKeys();
        } else if (this.unprefix("\\\\?(?:callable|Closure)\\s*\\(\\s*")) {
            parsed = parseFunc();
        } else if (this.unprefix("([a-zA-Z\\\\_][a-zA-Z\\\\_0-9\\-]*)\\s*")) {
            // should be put after SomeClass::class check when it is implemented
            String fqn = this.lastMatch.get(1);
            parsed = som(new TClass(fqn, new ArrayList<>()));
        } else if (this.unprefix("(\\d+\\.\\+d+)")) {
            String value = this.lastMatch.get(1);
            parsed = som(new TPrimitive(PhpType.FLOAT, value));
        } else if (this.unprefix("(\\d+)")) {
            String value = this.lastMatch.get(1);
            parsed = som(new TPrimitive(PhpType.INT, value));
        } else if (this.unprefix("true\\b")) {
            parsed = som(new TPrimitive(PhpType.TRUE, "1"));
        } else if (this.unprefix("false\\b")) {
            parsed = som(new TPrimitive(PhpType.FALSE, ""));
        } else if (this.unprefix("['\"]")) {
            char quote = this.lastMatch.get(0).charAt(0);
            parsed = parseString(quote).map(value ->
                new TPrimitive(PhpType.STRING, value));
        } else {
            // TODO: support class constants, value-of<T>, key-of<T>
        }
        if (!parsed.has()) {
            //System.out.println("failed to parse psalm value - " + Tls.substr(getTextLeft(), 0, 20));
        }
        return parsed;
    }

    private Opt<? extends IType> parseMultiValue()
    {
        return parseSingleValue().map(first -> {
            ArrayList<IType> following = new ArrayList<>();
            while (this.unprefix("\\s*\\|\\s*")) {
                Opt<? extends IType> next = parseSingleValue();
                if (next.has()) {
                    following.add(next.unw());
                } else {
                    break;
                }
            }
            if (following.size() > 0) {
                following.add(0, first);
                return new TMulti(following);
            } else {
                return first;
            }
        });
    }

    public static Opt<T2<IType, String>> parse(String typeText)
    {
        PsalmTypeExprParser self = new PsalmTypeExprParser(typeText);
        return self.parseMultiValue()
            .map(t -> T2(t, self.getTextLeft()));
    }
}
