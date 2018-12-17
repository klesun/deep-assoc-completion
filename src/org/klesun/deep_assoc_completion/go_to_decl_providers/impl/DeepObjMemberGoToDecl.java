package org.klesun.deep_assoc_completion.go_to_decl_providers.impl;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MemberReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.completion_providers.ObjMemberPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.It;
import org.klesun.lang.Lang;

import java.util.Objects;

/**
 * for cases when built-in Type Provider failed to determine
 * class of object - try ourselves using the Deep Resolver
 */
public class DeepObjMemberGoToDecl extends Lang
{
    private static It<? extends PsiElement> resolveMember(PhpClass cls, String name, FuncCtx funcCtx)
    {
        return list(
            It(cls.getFields()).flt(f -> f.getName().equals(name)),
            It(cls.getMethods()).flt(m -> m.getName().equals(name)),
            ObjMemberPvdr.getMagicProps(cls, funcCtx)
                .flt(t -> name.equals(t.stringValue))
                .map(t -> t.definition)
        ).fap(a -> a).unq();
    }

    public static It<PsiElement> resolveDeclPsis(@NotNull PsiElement leaf, int mouseOffset, FuncCtx funcCtx)
    {
        return opt(leaf.getParent())
            .fop(toCast(MemberReference.class))
            .flt(mem -> mem.multiResolve(false).length == 0)
            .fap(mem -> opt(mem.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .map(exp -> funcCtx.findExprType(exp).wap(Mt::new))
                .fap(mt -> list(
                    ArrCtorRes.resolveMtCls(mt, mem.getProject())
                        .fap(cls -> resolveMember(cls, mem.getName(), funcCtx)),
                    mt.getAssignedProps()
                        .fap(prop -> prop.keyType.getTypes.get())
                        .flt(propt -> Objects.equals(propt.stringValue, mem.getName()))
                        .map(prop -> prop.definition)
                ).fap(a -> a))
            ).map(a -> a);
    }
}
