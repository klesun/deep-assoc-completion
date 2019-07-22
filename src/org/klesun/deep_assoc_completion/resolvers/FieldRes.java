package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocPropertyTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FieldImpl;
import com.jetbrains.php.lang.psi.elements.impl.FieldReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.mem_res.MemRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.AssRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.Assign;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.It;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

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

    private It<Assign> getAssignments(Field resolved, FieldReference fieldRef)
    {
        IExprCtx implCtx = ctx.subCtxEmpty();
        return opt(resolved.getContainingFile())
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
    }

    public static It<DeepType> declToExplTypes(Field resolved, IExprCtx memCtx)
    {
        IExprCtx implCtx = memCtx.subCtxEmpty();
        It<DeepType> defTs = Tls.cast(FieldImpl.class, resolved).itr()
            .map(fld -> fld.getDefaultValue())
            .fop(toCast(PhpExpression.class))
            .fap(def -> implCtx.findExprType(def));

        It<DeepType> docTs = opt(resolved.getDocComment())
            .fap(doc -> It.cnc(
                opt(doc.getVarTag())
                    .fap(docTag -> new DocParamRes(implCtx).resolve(docTag)),
                PsalmRes.resolveVar(doc, resolved.getName(), memCtx)
            ));

        It<DeepType> magicTs = Tls.cast(PhpDocProperty.class, resolved)
            .fap(prop -> opt(prop.getParent()))
            .cst(PhpDocPropertyTag.class)
            .fap(tag -> new DocParamRes(memCtx).resolve(tag));

        DeepType builtInTs = new DeepType(resolved, resolved.getType());

        return It.cnc(som(builtInTs), defTs, docTs, magicTs);
    }

    private It<DeepType> declsToTypes(FieldReferenceImpl fieldRef, It<Field> declarations)
    {
        IExprCtx memCtx = ctx.subCtxMem(fieldRef);
        return declarations
            .fap(resolved -> {
                It<DeepType> explTypes = declToExplTypes(resolved, memCtx);
                It<Assign> asses = getAssignments(resolved, fieldRef);
                return It.cnc(
                    explTypes,
                    AssRes.assignmentsToTypes(asses)
                );
            });
    }

    private It<DeepType> resolveMagicProp(PhpClass cls, IExprCtx exprCtx)
    {
        return It(cls.getMethods())
            .flt(m -> "__get".equals(m.getName()))
            .fap(m -> MethCallRes.findMethRetType(m).apply(exprCtx));
    }

    /**
     * includes both dynamic props and props belonging to class
     * not sure, but probably should use this in resolve() to remove duplicating code
     */
    public static It<Key> getPublicProps(Mt mt, Project proj, IExprCtx memCtx)
    {
        It<Key> declared = ArrCtorRes.resolveMtInstCls(mt, proj)
            .fap(cls -> cls.getFields())
            .flt(f -> !f.getModifier().isPrivate())
            .map(f -> {
                DeepType kt = new DeepType(f, PhpType.STRING, f.getName());
                KeyType keyType = KeyType.mt(som(kt), f);
                return new Key(keyType, f)
                    .addType(() -> new Mt(declToExplTypes(f, memCtx)), f.getType());
            });
        return It.cnc(mt.types.fap(t -> t.props.vls()), declared);
    }

    public It<Field> getBriefDecls(MemberReference fieldRef)
    {
        MemRes memRes = new MemRes(ctx);
        return It.frs(
            () -> opt(fieldRef)
                .flt(ref -> !ref.getText().startsWith("static::")) // IDEA is bad at static:: resolution
                .fap(ref -> It(ref.multiResolve(false)))
                .map(res -> res.getElement())
                .fop(toCast(Field.class)),
            () -> memRes.resolveCls(fieldRef)
                .fap(cls -> cls.getFields())
                .flt(f -> f.getName().equals(fieldRef.getName()))
                .flt(f -> {
                    Boolean isDeclConst = f.isConstant();
                    Boolean hasDollar = Tls.cast(FieldReference.class, fieldRef)
                        .any(fr -> !fr.isConstant());
                    Boolean isRefConst = !hasDollar && fieldRef.isStatic();
                    return isDeclConst == isRefConst;
                })
        );
    }

    public It<DeepType> resolve(FieldReferenceImpl fieldRef)
    {
        MemRes memRes = new MemRes(ctx);
        Tls.OnDemand<Mt> getObjMt = Tls.onDemand(() -> opt(fieldRef.getClassReference())
            .fop(ref -> Opt.fst(
                () -> ctx.getSelfType()
                    .flt(typ -> ref.getText().equals("static"))
                    .flt(typ -> ArrCtorRes.resolveIdeaTypeCls(typ, ref.getProject()).has())
                    .map(typ -> new Mt(list(new DeepType(ref, typ)))),
                () -> opt(ctx.findExprType(ref).wap(Mt::new))
            ))
            .def(Mt.INVALID_PSI));

        // declarations taken from IDEA type, without deep resolution,
        // since it would be very long in a laravel project otherwise
        It<Field> briefDecls = getBriefDecls(fieldRef);
        It<DeepType> dynamicPropTs = It(list());
        It<DeepType> magicPropTs = It(list());
        if (!briefDecls.has() || getObjMt.has()) {
            String name = fieldRef.getName();
            name = "".equals(name) ? null : name;
            if (name == null) {
                name = It(fieldRef.getChildren())
                    .flt((c,i) -> i > 0) // skip first psi, it is the object var
                    .cst(Variable.class)
                    .fap(vari -> ctx.limitResolveDepth(5, vari))
                    .wap(Mt::getStringValueSt);
            }
            String finalName = name;
            dynamicPropTs = getObjMt.get().types
                .fap(t -> Mt.getDynaPropSt(t, finalName));
            IExprCtx magicCtx = ctx.subCtxMagicProp(fieldRef);
            magicPropTs = memRes.resolveCls(fieldRef)
                .fap(cls -> opt(fieldRef.getName())
                    .fap(nme -> It.cnc(
                        resolveMagicProp(cls, magicCtx),
                        opt(cls.getDocComment())
                            .fap(doc -> PsalmRes.resolveMagicProp(doc, nme, ctx.subCtxEmpty()))
                    )));
        }
        It<DeepType> declTypes = declsToTypes(fieldRef, briefDecls);

        return It.cnc(dynamicPropTs, magicPropTs, declTypes);
    }
}
