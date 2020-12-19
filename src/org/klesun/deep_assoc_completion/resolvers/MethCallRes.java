package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocMethod;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocMethodTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.FieldImpl;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionScalarArgument;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedReturnValuesIndex;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.entry.DeepSettings;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.builtins.MysqliRes;
import org.klesun.deep_assoc_completion.resolvers.mem_res.MemRes;
import org.klesun.deep_assoc_completion.resolvers.var_res.DocParamRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.lang.*;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

public class MethCallRes extends Lang
{
    final private IExprCtx ctx;

    public MethCallRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    public static boolean nameIs(MethodReferenceImpl call, String cls, String mth)
    {
        String callCls = opt(call.getClassReference())
            .map(clsPsi -> clsPsi.getName()).def("");
        String callMet = opt(call.getName()).def("");

        return callCls.equals(cls) && callMet.equals(mth);
    }

    public static L<PhpClass> getSupersAllowDupeFqn(PhpClass cls) {
        String superName = cls.getSuperFQN();
        PhpIndex phpIndex = PhpIndex.getInstance(cls.getProject());

        return It.cnc(
            phpIndex.getClassesByFQN(superName),
            It(cls.getImplementedInterfaces()),
            It(cls.getTraits())
        ).arr();
    }

    public static It<Method> findOverridingMethods(Method meth)
    {
        return opt(PhpIndex.getInstance(meth.getProject()))
            .fop(idx -> opt(meth.getContainingClass())
                .map(cls -> idx.getAllSubclasses(cls.getFQN())))
            .fap(clses -> clses)
            .fop(cls -> opt(cls.findMethodByName(meth.getName())));
    }

    public static It<Method> findOverriddenMethods(Method meth)
    {
        It<Method> overridden = opt(meth.getContainingClass())
            .fap(MethCallRes::getSupersAllowDupeFqn)
            .fap(PhpClass::getMethods)
            .flt(m -> m.getName().equals(meth.getName()));
        return It.cnc(som(meth), overridden);
    }

    /** note, does not include the overriding field unlike findOverriddenMethods() */
    public static It<Field> findOverriddenFields(FieldImpl caretField)
    {
        return opt(caretField.getContainingClass())
            .fap(MethCallRes::getSupersAllowDupeFqn)
            .fap(clsArg -> clsArg.getFields())
            .flt(m -> m.getName().equals(caretField.getName()));
    }

    /**
     * specific to the custom ORM framework used at my job...
     * would be nice to be able to make such stuff configurable on day
     */
    public It<DeepType> getCmsModelRowType(MethodReference methCall, Method meth)
    {
        // treating any class named "Model" as a base ORM class for Doctrine/Eloquent/CustomStuff completion
        String clsNme = opt(meth.getContainingClass()).map(cls -> cls.getName()).def("");
        return Tls.ifi(
            clsNme.equals("Model") && meth.getName().equals("get"),
            () -> new MiscRes(ctx).resolveClassReferenceFromMember(methCall)
        ).fap(ideaType -> {
            Mutable<Boolean> isAssoc = new Mutable<>(false);
            It<T2<String, PsiElement>> fieldNames =
                ArrCtorRes.resolveIdeaTypeCls(ideaType, methCall.getProject())
                .fap(callCls -> It.cnc(som(callCls), It(callCls.getSupers())).end(sup -> sup.getName().equals("Model")).unq())
                .fap(callCls -> callCls.getFields())
                // could also add here "getFields" functions, not just "fields" property
                .flt(f -> f.getName().equals("fields"))
                .fap(f -> opt(f.getDefaultValue()))
                .fop(toCast(PhpExpression.class))
                .fap(val -> ctx.limitResolveDepth(15, val))
                .fap(valt -> valt.keys)
                .btw(k -> isAssoc.set(It(k.keyType.getTypes())
                    .fst().any(kt -> !kt.isNumber())))
                .fap(k -> {
                    It<DeepType> keyTypes = isAssoc.get()
                        ? It(k.keyType.getTypes())
                        : k.getValueTypes();
                    return keyTypes.fap(t -> opt(t.stringValue).map(str -> T2(str, t.definition)));
                })
                .unq(t2 -> t2.a);
            return It.cnc(
                som(new DeepType(methCall, ideaType)),
                fieldNames.map(nme -> UsageBasedTypeResolver.makeAssoc(nme.b, som(nme)))
            );
        });
    }

    private static It<? extends Function> getFuncByFqn(String fullFqn, PhpIndex phpIdx)
    {
        String[] parts = fullFqn.split("\\.");
        if (parts.length == 2) {
            // a method
            String cls = parts[0];
            String meth = parts[1];
            return It(phpIdx.getAnyByFQN(cls))
                .fap(clsPsi -> clsPsi.getMethods())
                .flt(methPsi -> meth.equals(methPsi.getName()));
        } else {
            // plain function in global scope
            return It(phpIdx.getFunctionsByFQN(fullFqn));
        }
    }

    public static IIt<DeepType> findFqnMetaType(
        String fqn, IExprCtx ctx,
        ID<String, Collection<PhpExpectedFunctionArgument>> idxKey,
        Predicate<PhpExpectedFunctionArgument> argCond
    ) {
        return ctx.getProject().rap(proj -> {
            FileBasedIndex index = FileBasedIndex.getInstance();
            PhpIndex phpIdx = PhpIndex.getInstance(proj);
            GlobalSearchScope scope = GlobalSearchScope.allScope(proj);
            return list(fqn, fqn.replaceAll("^\\\\", ""))
                .rap(fullFqn -> L(index.getValues(idxKey, fullFqn, scope))
                    .rap(Lang::L)
                    .flt(argCond).arr()
                    .cst(PhpExpectedFunctionScalarArgument.class)
                    .unq().arr().rap(exp -> {
                        Opt<? extends PsiElement> declOpt = Opt.fst(
                            () -> opt(exp.getNamedElement(proj)),
                            () -> getFuncByFqn(fqn, phpIdx).fst().map(a -> a)
                        );
                        IExprCtx docCtx = declOpt
                            .map(ctx::subCtxDoc)
                            .def(ctx.subCtxEmpty());
                        return DocParamRes.parseExpression(exp.getValue(), ctx.getProject().unw(), docCtx);
                    }));
        });
    }

    public static IIt<DeepType> findFqnMetaDefRetType(String fqn, IExprCtx ctx)
    {
        try {
            return findFqnMetaType(fqn, ctx, PhpExpectedReturnValuesIndex.KEY, arg -> true);
        } catch (NoClassDefFoundError exc) {
            // can happen due to wrong phpstorm version in my plugin.xml
            return non();
        }
    }

    private It<DeepType> findBuiltInRetType(Method meth, IExprCtx argCtx, MethodReference methCall)
    {
        It<DeepType> types = new MysqliRes(ctx).resolveOopCall(meth, argCtx, methCall);
        It<DeepType> modelRowTypes = getCmsModelRowType(methCall, meth);
        It<DeepType> modelRowArrTypes = !modelRowTypes.has() ? It.non() :
            It(som(Mt.getInArraySt(modelRowTypes, methCall)));
        types = It.cnc(types, modelRowArrTypes);
        return types;
    }

    private static It<DeepType> parseReturnDoc(PhpDocTag returnDoc, IExprCtx funcCtx)
    {
        IExprCtx docCtx = funcCtx.subCtxDoc(returnDoc);
        String regex = "^\\s*(like|=|)\\s*((?:\\[|\\\\?[a-zA-Z_]+[\\(:]|new\\s+).*)$";
        It<DeepType> asEq = Tls.regex(regex, returnDoc.getTagValue())
            .fop(match -> match.gat(1))
            .fap(expr -> DocParamRes.parseExpression(expr, returnDoc.getProject(), docCtx));
        IIt<DeepType> asPsalm = PsalmRes.resolveReturn(returnDoc, funcCtx);
        L<String> typeStrings = opt(returnDoc.getDocType())
            .fap(PhpType::getTypes).arr();
        // phpstorm resolves explicit types ok, but not static, naturally
        It<DeepType> asStatic = !typeStrings.contains("static")
            ? It.non() : It.cnc(
                funcCtx.getSelfType().map(clst -> new DeepType(returnDoc, clst)),
                funcCtx.getThisType()
            );
        return It.cnc(asStatic, asEq, asPsalm);
    }

    private static It<DeepType> parseMethDoc(PhpDocMethod doc, IExprCtx ctx)
    {
        return opt(doc.getParent())
            .cst(PhpDocMethodTag.class)
            .fap(tag -> {
                L<PsiElement> tagParts = L(tag.getChildren());
                L<PsiElement> methAndRest = L(tagParts).sub(-2);
                // text after signature _on same line_
                final String descrPart;
                if (methAndRest.size() == 2 &&
                    methAndRest.get(0) instanceof PhpDocMethod &&
                    methAndRest.get(0).getText().endsWith(")")
                ) {
                    descrPart = methAndRest.get(1).getText();
                } else {
                    // before around ending of 2019 @method psi
                    // contents were not structured much
                    descrPart = tagParts
                        .flt(psi -> (psi + "").equals("DOC_METHOD_DESCR"))
                        .map(psi -> psi.getText()).str("");
                }
                // text on following lines
                String valuePart = tag.getTagValue();
                String fullDescr = descrPart + "\n" + valuePart;
                return new DocParamRes(ctx)
                    .parseEqExpression(fullDescr, doc);
            });
    }

    public static It<DeepType> findDocRetType(PhpDocComment doc, IExprCtx ctx)
    {
        return It.cnc(
            opt(doc.getReturnTag()),
            It(doc.getChildren()).cst(PhpDocTag.class)
                .flt(t -> "@psalm-return".equals(t.getName()))
        ).fap(tag -> parseReturnDoc(tag, ctx));
    }

    public static It<DeepType> findFuncDocRetType(Function func, IExprCtx ctx)
    {
        It<Function> redecls = Tls.cast(Method.class, func)
            .fap(m -> findOverriddenMethods(m))
            .map(m -> (Function)m)
            .orr(som(func));
        return redecls
            .fap(m -> It.cnc(
                opt(m.getDocComment()).fap(doc -> findDocRetType(doc, ctx)),
                findFqnMetaDefRetType(m.getFQN(), ctx)
            ));
    }

    private It<DeepType> resolveMagicMethCustomDocFormat(PhpClass clsPsi, String methName)
    {
        return opt(clsPsi.getDocComment())
            .fap(doc -> PsalmRes.resolveMagicReturn(doc, methName, ctx));
    }

    public static F<IExprCtx, It<DeepType>> findMethRetType(Method meth)
    {
        return (IExprCtx fullCtx) -> {
            L<Method> impls = list(meth);
            IExprCtx implCtx = fullCtx;
            if (meth.isAbstract()) {
                impls = It.cnc(list(meth), findOverridingMethods(meth)).arr();
                // ignore $this and args in implementations
                // since there may be dozens of them (Laravel)
                // ... maybe should not look for implementations if return
                // type is explicitly stated to be a class, not array?
                if (!DeepSettings.inst(meth.getProject()).passArgsToImplementations) {
                    implCtx = fullCtx.subCtxEmpty();
                }
            }
            IExprCtx finalImplCtx = implCtx;
            It<DeepType> docTit = findFuncDocRetType(meth, fullCtx);
            It<DeepType> magicDocTit = Tls.cast(PhpDocMethod.class, meth)
                .fap(doc -> parseMethDoc(doc, fullCtx));
            It<DeepType> implTit = impls.fap(m -> It.cnc(
                opt(m.getReturnType()).fap(rt -> list(new DeepType(rt, rt.getType()))),
                ClosRes.getReturnedValue(m, finalImplCtx)
            ));
            return It.cnc(docTit, magicDocTit, implTit);
        };
    }

    public static It<Method> resolveMethodsNoNs(MethodReference call, IExprCtx ctx)
    {
        String cls = opt(call.getClassReference()).map(c -> c.getText()).def("");
        String mth = opt(call.getName()).def("");
        return resolveMethodsNoNs(cls, mth, call.getProject());
    }

    private static It<Method> resolveMethodsNoNs(String partialFqn, String func, Project proj)
    {
        return MemRes.findClsByFqnPart(partialFqn, proj)
            .fap(cls -> cls.getMethods())
            .flt(m -> Objects.equals(m.getName(), func));
    }

    private It<Method> findReferenced(@Nullable String methName, MemIt<PhpClass> clses)
    {
        It<Method> tit = clses
            .fap(cls -> cls.getMethods())
            .flt(f -> f.getName().equals(methName));
        return tit;
    }

    private It<Method> resolveMethodFromCall(MethodReferenceImpl call, MemIt<PhpClass> clses)
    {
        return It.frs(
            () -> findReferenced(call.getName(), clses),
            () -> It(call.multiResolve(false))
                .fap(v -> opt(v.getElement()))
                .cst(Method.class),
            () -> resolveMethodsNoNs(call, ctx)
        );
    }

    private It<DeepType> resolveMagicFqn(MethodReferenceImpl methCall, IExprCtx callCtx)
    {
        String clsFqn = opt(methCall.getClassReference())
            .cst(ClassReferenceImpl.class)
            .map(ref -> ref.getFQN())
            .def("");
        String methName = opt(methCall.getName()).def("");
        if ((clsFqn.equals("Magic") || clsFqn.equals("\\Magic")) &&
            methName.equals("dbRow")
        ) {
            return callCtx.func().getArgMt(0).types
                .fap(t -> opt(t.stringValue)).unq()
                .fap(table -> MysqliRes.getTableColumns(table, methCall.getProject()))
                .map(keyName -> Mkt.assoc(methCall, list(T2(keyName, Mkt.str(methCall).mt()))));
        } else {
            return It.non();
        }
    }

    public It<DeepType> resolveCall(MethodReferenceImpl funcCall)
    {
        IExprCtx funcCtx = ctx.subCtxDirect(funcCall);
        MemIt<PhpClass> clses = new MemRes(ctx).resolveCls(funcCall).unq().mem();

        It<DeepType> noDeclTit = UsageBasedTypeResolver.getCallFqn(funcCall)
            .fap(fqn -> findFqnMetaDefRetType(fqn, ctx));
        It<DeepType> asCustomFormatClsDoc = opt(funcCall.getName())
            .fap(nme -> clses.fap(cls -> resolveMagicMethCustomDocFormat(cls, nme)));
        It<DeepType> declTit = resolveMethodFromCall(funcCall, clses)
            .fap(func -> It.cnc(
                findMethRetType(func).apply(funcCtx),
                findBuiltInRetType(func, funcCtx, funcCall)
            ));
        It<DeepType> magicFqnTit = resolveMagicFqn(funcCall, funcCtx);

        return It.cnc(noDeclTit, asCustomFormatClsDoc, declTit, magicFqnTit);
    }
}
