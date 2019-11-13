package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.BinaryExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.NewExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.TernaryExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.psalm.PsalmFuncInfo;
import org.klesun.lang.*;

/**
 * simple expressions that can be supported with
 * just few lines of code are gathered here
 */
public class MiscRes extends Lang
{
    final private IExprCtx ctx;

    public MiscRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private It<DeepType> findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .fap(casted -> ctx.findExprType(casted));
    }

    private It<PhpType> resolveAsGeneric(PsiElement docPsi, String name)
    {
        return Tls.findParent(docPsi, PhpClass.class)
            .fap(clsPsi -> opt(clsPsi.getDocComment()))
            .fap(doc -> {
                PsalmFuncInfo.PsalmClsInfo clsInfo = PsalmFuncInfo.parseClsDoc(doc);
                return clsInfo.generics.fap((gen, i) -> gen.name
                    .equals(name) ? som(i) : non());
            })
            .fap(genPos -> ctx.getThisType()
                .fap(t -> t.generics.gat(genPos)))
            .fap(mt -> mt.types)
            .map(t -> t.briefType);
    }

    public It<PhpType> resolveAnyClassReference(PhpExpression clsRefPsi, boolean ideaKnows)
    {
        return It.frs(
            // new static()
            () -> opt(clsRefPsi)
                .flt(ref -> ref.getText().equals("static")).itr()
                .fap(ref -> It.frs(
                    () -> ctx.getSelfType().fap(pst -> ArrCtorRes.filterObjPst(pst)),
                    () -> ctx.getThisType().fap(t -> ArrCtorRes.filterObjPst(t.briefType)),
                    () -> Tls.findParent(ref, PhpClass.class, a -> true)
                        .map(cls -> cls.getType())
                )),
            // new SomeCls(), $someObj->someField
            () -> opt(clsRefPsi)
                // don't allow IDEA to resolve $someCls::doSomething(), since it loses static:: context
                .flt(ref -> !ref.getText().startsWith("$"))
                .fap(ref -> {
                    System.out.println("zhopa ctx " + ctx + " " + ctx.isInComment());
                    if (ideaKnows) {
                        return som(ref.getType());
                    } else {
                        return ctx.getFakeFileSource()
                            .fap(funcDoc -> resolveAsGeneric(funcDoc, ref.getText()));
                    }
                }),
            // new $clsInAVar()
            () -> opt(clsRefPsi)
                .fap(ref -> ctx.findExprType(ref))
                .fap(t -> t.clsRefType)
        );
    }

    public It<PhpType> resolveClassReference(PsiPolyVariantReference poly, PhpExpression clsRefPsi)
    {
        return resolveAnyClassReference(clsRefPsi, It(poly.multiResolve(false)).has());
    }

    public It<PhpType> resolveClassReferenceFromMember(MemberReference memRef)
    {
        return opt(memRef.getClassReference())
            .fap(ref -> resolveAnyClassReference(ref, It(memRef.multiResolve(false)).has()));
    }

    private It<DeepType> resolveNew(NewExpression newExp)
    {
        return opt(newExp.getClassReference())
            .fap(cls -> resolveClassReference(cls, cls))
            .map(ideaType -> {
                IExprCtx ctorArgs = ctx.subCtxDirect(newExp);
                L<Mt> generics = ArrCtorRes.resolveIdeaTypeCls(ideaType, newExp.getProject())
                    .fap(clsPsi -> clsPsi.getMethods())
                    .flt(m -> m.getName().equals("__construct"))
                    .fap(ctor -> opt(ctor.getDocComment()))
                    .map(ctorDoc -> PsalmFuncInfo.parse(ctorDoc))
                    .map(ctorInfo -> ctorInfo.classGenerics
                        .map(g -> PsalmRes.getGenTypeFromFunc(
                            g, ctorInfo.params, ctorArgs, newExp
                        ).wap(Mt::new))
                        .arr())
                    .flt(gens -> gens.size() > 0)
                    .fst()
                    .def(L());

                return DeepType.makeNew(newExp, ctorArgs, generics, ideaType);
            });
    }

    private It<DeepType> resolveInclude(Include casted)
    {
        return opt(casted.getArgument())
            .cst(PhpExpression.class)
            .fap(expr -> ctx.findExprType(expr))
            .fap(t -> opt(t.stringValue))
            .fap(path -> opt(LocalFileSystem.getInstance().findFileByPath(path)))
            .fap(f -> opt(PsiManager.getInstance(casted.getProject()).findFile(f)))
            .fap(f -> Tls.findChildren(f, GroupStatement.class).fst())
            .fap(block -> ClosRes.findFunctionReturns(block))
            .fap(ret -> opt(ret.getArgument()))
            .cst(PhpExpression.class)
            .fap(arg -> ctx.subCtxEmpty().findExprType(arg));
    }

    private L<String> getCastTypes()
    {
        return list(
            "int",
            "integer",
            "bool",
            "boolean",
            "float",
            "double",
            "real",
            "string",
            "array",
            "object",
            "unset"
        );
    }

    private static Opt<DeepType> castToPhpType(DeepType deepType, String phpType)
    {
        if (phpType.equals("string")) {
            return som(new DeepType(deepType.definition, PhpType.STRING, deepType.stringValue));
        } else if (phpType.equals("int") || phpType.equals("integer")) {
            return som(DeepType.makeInt(deepType.definition, deepType.stringValue));
        } else if (phpType.equals("array")) {
            DeepType arrt = new DeepType(deepType.definition, PhpType.ARRAY);
            arrt.keys = It.cnc(deepType.keys, deepType.props.vls()).mem();
            return som(arrt);
        } else if (phpType.equals("object")) {
            DeepType objt = new DeepType(deepType.definition, PhpType.OBJECT);
            deepType.props.vls().cct(deepType.keys)
                .fch(k -> k.keyType.getNames()
                    .fch(n -> objt.addProp(n, k.definition)
                        .addType(() -> new Mt(k.getValueTypes()))));
            return som(objt);
        } else if (phpType.equals("bool") || phpType.equals("boolean")) {
            return som(new DeepType(deepType.definition, PhpType.BOOLEAN, deepType.stringValue));
        } else if (phpType.equals("float") || phpType.equals("double") || phpType.equals("real")) {
            return som(new DeepType(deepType.definition, PhpType.FLOAT, deepType.stringValue));
        } else if (phpType.equals("unset")) {
            return som(new DeepType(deepType.definition, PhpType.NULL));
        } else {
            return non();
        }
    }

    public It<DeepType> resolve(PsiElement expr)
    {
        return It.cnc(It.non()
            , Tls.cast(TernaryExpressionImpl.class, expr)
                .fap(tern -> It.cnc(
                    findPsiExprType(tern.getTrueVariant()),
                    findPsiExprType(tern.getFalseVariant())
                ))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                .fap(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("??") || op.getText().equals("?:"))
                    .fap(op -> It.cnc(
                        findPsiExprType(bin.getLeftOperand()),
                        findPsiExprType(bin.getRightOperand())
                    )))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                .fap(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("-")
                        || op.getText().equals("*") || op.getText().equals("/")
                        || op.getText().equals("%") || op.getText().equals("**")
                    )
                    .fap(op -> {
                        DeepType type = new DeepType(bin, PhpType.NUMBER);
                        type.isNumber = true;
                        return list(type);
                    }))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                .fap(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("+"))
                    .fap(op -> {
                        It<DeepType> tit = It.cnc(
                            findPsiExprType(bin.getLeftOperand()),
                            findPsiExprType(bin.getRightOperand())
                        );
                        Mutable<Boolean> isNum = new Mutable<>(false);
                        return tit.map(t -> {
                            isNum.set(isNum.get() || t.isNumber);
                            return isNum.get() ? new DeepType(bin, PhpType.NUMBER) : t;
                        });
                    }))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                .fap(bin -> opt(bin.getOperation())
                    .flt(op -> op.getText().equals("."))
                    .fap(op -> {
                        It<DeepType> lmt = findPsiExprType(bin.getLeftOperand());
                        It<DeepType> rmt = findPsiExprType(bin.getRightOperand());
                        String unescaped = opt(Mt.getStringValueSt(lmt))
                            .fop(lstr -> opt(Mt.getStringValueSt(rmt))
                                .map(rstr -> lstr + rstr))
                            .map(ccted -> StringEscapeUtils.unescapeJava(ccted)) // PHP ~ java
                            .def(null);
                        DeepType type = new DeepType(bin, PhpType.STRING, unescaped);
                        return list(type);
                    }))
            , Tls.cast(UnaryExpression.class, expr)
                .fap(bin -> opt(bin.getOperation())
                    .fap(op -> Tls.regex("^\\(\\s*(" + Tls.implode("|", getCastTypes()) + ")\\s*\\)$", op.getText()))
                    .fap(m -> m.fst())
                    .fap(phpType -> findPsiExprType(bin.getValue())
                        .fap(t -> castToPhpType(t, phpType))))
            , Tls.cast(NewExpressionImpl.class, expr)
                .fap(newExp -> resolveNew(newExp))
            , Tls.cast(Include.class, expr)
                .fap(casted -> resolveInclude(casted))
        );
    }

}
