package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.*;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.lang.*;

import java.util.Collection;
import java.util.Set;

public class FieldRes extends Lang
{
    final private IExprCtx ctx;

    public FieldRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private It<FieldReferenceImpl> findReferences(PsiFile file, String name)
    {
        // ReferenceSearch seems to cause freezes
//        SearchScope scope = GlobalSearchScope.fileScope(
//            fieldRef.getProject(),
//            decl.getContainingFile().getVirtualFile()
//        );
//        return ReferencesSearch.search(decl, scope, false).findAll();

        // this takes 6 milliseconds on just ApolloPnrFieldsOnDemand.php each time
        // maybe it can be done more efficiently like iterating manually (could return iterator in such case BTW), caching or reusing IDEA's reference provider
        // but if I remember correctly, IDEA's reference resolver was not used here because it randomly threw exceptions
        if (!ctx.getFieldRefCache().containsKey(file)) {
            long startTime = System.nanoTime();
            ctx.getFieldRefCache().put(file, PsiTreeUtil.findChildrenOfType(file, FieldReferenceImpl.class));
            double elapsed = (System.nanoTime() - startTime) / 1000000000.0;
            //System.out.println("found refs in " + file.getName() + " over " + elapsed + " seconds");
        }
        Collection<FieldReferenceImpl> refs = ctx.getFieldRefCache().get(file);
        return It(refs)
            .flt(ref -> name.equals(ref.getName()));
    }

    private static boolean areInSameScope(PsiElement a, PsiElement b)
    {
        Opt<Function> aFunc = Tls.findParent(a, Function.class, v -> true);
        Opt<Function> bFunc = Tls.findParent(b, Function.class, v -> true);
        return aFunc.equals(bFunc);
    }

    // when you do instanceof, IDEA type acquires the class, so  it may
    // happen that args passed to an instance as instance of one class
    // could be used in some other instanceof-ed class if we don't do this check
    private static boolean isSameClass(IExprCtx ctx, PhpClass fieldCls)
    {
        // return true if ctx class is empty or ctx class constructor is in fieldCls
        // (if fieldCls is ctx class or ctx class inherits constructor from fieldCls)
        Set<String> ctxFqns = ArrCtorRes.ideaTypeToFqn(ctx.getSelfType().def(PhpType.UNSET));
        return ctxFqns.isEmpty() || ctxFqns.contains(fieldCls.getFQN());
    }

    public It<DeepType> resolve(FieldReferenceImpl fieldRef)
    {
        S<Mt> getObjMt = Tls.onDemand(() -> opt(fieldRef.getClassReference())
            .fop(ref -> Opt.fst(
                () -> ctx.getSelfType()
                    .flt(typ -> ref.getText().equals("static"))
                    .flt(typ -> ArrCtorRes.resolveIdeaTypeCls(typ, ref.getProject()).has())
                    .map(typ -> new Mt(list(new DeepType(ref, typ)))),
                () -> opt(ctx.findExprType(ref).wap(Mt::new))
            ))
            .def(Mt.INVALID_PSI));

        It<Field> declarations = It.frs(
            () -> opt(fieldRef)
                .flt(ref -> !ref.getText().startsWith("static::")) // IDEA is bad at static:: resolution
                .fap(ref -> It(ref.multiResolve(false)))
                .map(res -> res.getElement())
                .fop(toCast(Field.class)),
            () -> opt(getObjMt.get())
                .fap(mt -> ArrCtorRes.resolveMtCls(mt, fieldRef.getProject()))
                .fap(cls -> cls.getFields())
                .flt(f -> f.getName().equals(fieldRef.getName()))
        );
        It<DeepType> propDocTs = It(list());
        if (!declarations.has()) {
            propDocTs = getObjMt.get().getProps()
                .flt(prop -> prop.keyType.getNames().any(n -> n.equals(fieldRef.getName())))
                .fap(prop -> prop.getTypes());
        }
        It<DeepType> declTypes = declarations
            .fap(resolved -> {
                IExprCtx implCtx = ctx.subCtxEmpty();
                It<DeepType> defTs = Tls.cast(FieldImpl.class, resolved).itr()
                    .map(fld -> fld.getDefaultValue())
                    .fop(toCast(PhpExpression.class))
                    .fap(def -> implCtx.findExprType(def));

                It<DeepType> docTs = opt(resolved.getContainingClass()).itr()
                    .fop(cls -> opt(cls.getDocComment()))
                    .fap(doc -> doc.getPropertyTags())
                    .flt(tag -> opt(tag.getProperty()).flt(pro -> pro.getName().equals(fieldRef.getName())).has())
                    .fap(tag -> new DocParamRes(ctx).resolve(tag));

                It<Assign> asses = opt(resolved.getContainingFile()).itr()
                    .fap(file -> findReferences(file, fieldRef.getName()))
                    .fap(assPsi -> Tls.findParent(assPsi, Method.class, a -> true)
                        .flt(meth -> meth.getName().equals("__construct"))
                        .map(meth -> fieldRef.getClassReference())
                        .fop(toCast(PhpExpression.class))
                        .fap(ref -> ctx.findExprType(ref))
                        .fop(t -> t.ctorArgs)
                        .flt(ctx -> opt(resolved.getContainingClass())
                            .map(cls -> isSameClass(ctx, cls)).def(true))
                        .wap(ctxs -> It.cnc(
                            ctxs,
                            Tls.ifi(areInSameScope(fieldRef, assPsi), () -> list(ctx)),
                            Tls.ifi(!ctxs.has(), () -> list(implCtx))
                        ))
                        .fop(methCtx -> (new AssRes(methCtx)).collectAssignment(assPsi, false)));

                return It.cnc(
                    defTs, docTs,
                    AssRes.assignmentsToTypes(asses)
                );
            });

        return It.cnc(propDocTs, declTypes);
    }
}
