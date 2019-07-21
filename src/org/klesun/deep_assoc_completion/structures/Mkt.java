package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.DirectTypeResolver;
import org.klesun.lang.It;
import org.klesun.lang.Opt;

import static org.klesun.lang.Lang.*;

/** short for "Make Type" */
public class Mkt {
    public static DeepType str(PsiElement psi, String content)
    {
        return new DeepType(psi, PhpType.STRING, content);
    }

    public static DeepType str(PsiElement psi)
    {
        return new DeepType(psi, PhpType.STRING);
    }

    public static DeepType inte(PsiElement psi)
    {
        return new DeepType(psi, PhpType.INT);
    }

    public static DeepType inte(PsiElement psi, Integer content)
    {
        return new DeepType(psi, PhpType.INT, content + "");
    }

    public static DeepType inte(PsiElement psi, String content)
    {
        return new DeepType(psi, PhpType.INT, content + "");
    }

    public static DeepType floate(PsiElement psi)
    {
        return new DeepType(psi, PhpType.FLOAT);
    }

    public static DeepType floate(PsiElement psi, double content)
    {
        return new DeepType(psi, PhpType.FLOAT, content + "");
    }

    public static DeepType bool(PsiElement psi)
    {
        return new DeepType(psi, PhpType.BOOLEAN);
    }

    public static DeepType bool(PsiElement psi, Boolean content)
    {
        return new DeepType(psi, PhpType.BOOLEAN, (content ? 1 : 0) + "");
    }

    public static DeepType res(PsiElement psi)
    {
        return new DeepType(psi, PhpType.RESOURCE);
    }

    public static DeepType callable(PsiElement psi)
    {
        return new DeepType(psi, PhpType.CALLABLE);
    }

    public static DeepType arr(PsiElement psi)
    {
        return new DeepType(psi, PhpType.ARRAY);
    }

    public static DeepType arr(PsiElement psi, Mt elt)
    {
        return elt.getInArray(psi);
    }

    public static DeepType assoc(PsiElement psi, Iterable<T2<String, Mt>> keys)
    {
        It<Key> keyEntries = It(keys)
            .map(tup -> tup.nme((name, valMt) -> {
                PhpType ideaType = valMt.getIdeaTypes().fst().def(PhpType.UNSET);
                return new Key(name, psi).addType(Granted(valMt), ideaType);
            }));
        return new Build(psi, PhpType.ARRAY)
            .isExactPsi(false)
            .keys(keyEntries)
            .get();
    }

    public static DeepType assocCmnt(PsiElement psi, Iterable<T3<String, Mt, Opt<String>>> keys)
    {
        It<Key> keyEntries = It(keys)
            .map(tup -> tup.nme((keyName, mt, cmnt) -> {
                PhpType ideaType = mt.getIdeaTypes().fst().def(PhpType.UNSET);
                return new Key(keyName, psi)
                    .addType(Granted(mt), ideaType).addComments(cmnt);
            }));
        return new Build(psi, PhpType.ARRAY)
            .isExactPsi(false)
            .keys(keyEntries)
            .get();
    }

    public static It<DeepType> cst(IExprCtx ctx, Iterable<String> cstNames)
    {
        return ctx.getProject().map(p -> PhpIndex.getInstance(p))
            .fap(idx -> It(cstNames).fap(nme -> It(idx.getConstantsByName(nme)))
                .fap(cstDef -> DirectTypeResolver.resolveConst(cstDef, ctx)));
    }

    public static DeepType mixed(PsiElement psi)
    {
        return new DeepType(psi, PhpType.MIXED);
    }
}
