package org.klesun.deep_assoc_completion.structures.psalm;

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
public class PsalmParser
{
    final private String text;
    private int offset = 0;
    private L<String> lastMatch = new L<>();

    private PsalmParser(String text)
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
            Opt<? extends IType> typeOpt = parseValue();
            if (typeOpt.has()) {
                types.add(typeOpt.unw());
            } else {
                return non();
            }
        } while (this.unprefix("\\s*,\\s*"));
        return som(types);
    }

    private Opt<LinkedHashMap<String, IType>> parseKeys()
    {
        LinkedHashMap<String, IType> keys = new LinkedHashMap<>();
        while (this.unprefix("\\s*,?\\s*(\\w+)\\s*:\\s*")) {
            String keyName = this.lastMatch.get(1);
            Opt<? extends IType> typeOpt = parseValue();
            if (typeOpt.has()) {
                keys.put(keyName, typeOpt.unw());
            } else {
                return non();
            }
        }
        return som(keys);
    }

    private Opt<? extends IType> parseValue()
    {
        this.unprefix("\\s+");
        Opt<? extends IType> parsed = non();
        if (this.unprefix("([a-zA-Z\\\\_][a-zA-Z\\\\_0-9]*)\\s*<\\s*")) {
            String fqn = this.lastMatch.get(1);
            parsed = parseTypeList()
                .map(generics -> new TClass(fqn, generics))
                .flt(t -> unprefix("\\s*>\\s*"));
        } else if (this.unprefix("array\\s*\\{\\s*")) {
            parsed = parseKeys()
                .map(keys -> new TAssoc(keys))
                .flt(t -> {
                    unprefix("\\s*,\\s*"); // optional trailing coma
                    return unprefix("\\s*}\\s*");
                });
        } else if (this.unprefix("\\s*([a-zA-Z\\\\_][a-zA-Z\\\\_0-9]*)\\s*")) {
            // should be put after SomeClass::class check when it is implemented
            String fqn = this.lastMatch.get(1);
            parsed = som(new TClass(fqn, new ArrayList<>()));
        } else {
            // TODO: support value types and union types like "Something::class", "'priceItinerary'", "3.14|null", etc...
        }
        if (!parsed.has()) {
            //System.out.println("failed to parse psalm value - " + Tls.substr(getTextLeft(), 0, 20));
        }
        return parsed;
    }

    public static Opt<T2<IType, String>> parse(String typeText)
    {
        // TODO: would be nice to return what we managed to parse so far at least,
        //  that would help user to understand where is the mistake in his definition
        PsalmParser self = new PsalmParser(typeText);
        return self.parseValue()
            .map(t -> T2(t, self.getTextLeft()));
    }
}
