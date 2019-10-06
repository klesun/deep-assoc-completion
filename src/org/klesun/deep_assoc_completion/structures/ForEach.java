package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.ForeachImpl;
import org.klesun.deep_assoc_completion.resolvers.VarRes;
import org.klesun.lang.L;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import static org.klesun.lang.Lang.*;

/**
 * for some reason, phpstorm foreach psi structure is somewhat raw and does not match
 * the structure of normal list() assignment - encapsulating the normalization logic here
 */
public class ForEach
{
    public PhpExpression srcArr;
    public Opt<Variable> keyVar = non();
    public Opt<Variable> valVar = non();
    public L<Lang.T2<String, Variable>> listVars = Lang.L();

    private static boolean isInList(Variable lstVar)
    {
        PsiElement prev = lstVar.getPrevSibling();
        while (prev != null) {
            if (prev.getText().equals("list")) {
                return true;
            } else if (
                !list(",", "(").contains(prev.getText()) &&
                !(prev instanceof PsiWhiteSpaceImpl)
            ) {
                return false;
            } else {
                prev = prev.getPrevSibling();
            }
        }
        return false;
    }

    private static boolean isListDestr(PhpExpression srcArr)
    {
        return opt(srcArr.getNextSibling())
            .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getNextSibling()) : som(s))
            .fop(s -> s.getText().equals("as") ? opt(s.getNextSibling()) : non())
            .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getNextSibling()) : som(s))
            .any(s -> s.getText().equals("[") || s.getText().equals("list"));
    }

    private static L<T2<String, Variable>> parseListVars(L<Variable> tuple)
    {
        // would be nice to handle list(, , $var) as well at some point...
        return tuple.map((el, i) -> {
            Opt<String> keyOpt = opt(el.getPrevSibling())
                .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getPrevSibling()) : som(s))
                .fop(s -> s.getText().equals("=>") ? opt(s.getPrevSibling()) : non())
                .fop(s -> s instanceof PsiWhiteSpace ? opt(s.getPrevSibling()) : som(s))
                .cst(StringLiteralExpression.class)
                .map(s -> s.getContents());
            return T2(keyOpt.def(i + ""), el);
        }).arr();
    }

    // array, key, value, list values
    public static Opt<ForEach> parse(ForeachImpl fch)
    {
        return opt(fch.getArray())
            .fop(toCast(PhpExpression.class))
            .map(arr -> {
                ForEach self = new ForEach();
                self.srcArr = arr;

                L<Variable> tuple = L(fch.getVariables());
                self.keyVar = opt(fch.getKey())
                    // IDEA breaks on list() - should help her
                    .flt(keyVar -> Opt.fst(
                        () -> opt(keyVar.getNextSibling())
                            .fop(toCast(PsiWhiteSpace.class))
                            .map(ws -> ws.getNextSibling()),
                        () -> opt(keyVar.getNextSibling())
                    ).flt(par -> par.getText().equals("=>")).has());
                if (self.keyVar.has()) {
                    tuple = tuple.sub(1); // key was included
                }
                if (isListDestr(arr)) {
                    self.listVars = parseListVars(tuple);
                } else {
                    self.valVar = tuple.fst();
                }
                return self;
            });
    }
}
