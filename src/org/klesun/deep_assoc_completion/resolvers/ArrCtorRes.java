package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.ClassReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.mem_res.MemRes;
import org.klesun.deep_assoc_completion.structures.Build;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.Key;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ArrCtorRes extends Lang
{
    final private IExprCtx ctx;

    public ArrCtorRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    public static Opt<PhpType> filterObjPst(PhpType pst)
    {
        if (pst == null) {
            return non();
        }
        PhpType objt = PhpType.builder()
            .add(PhpType.OBJECT)
            .add(PhpType.$THIS)
            .add(PhpType.STATIC).build();
        Set<String> partiallyFiltered = pst.filterPrimitives()
            .filterNull().filterMixed().filter(objt).getTypes();
        L<String> filtered = L(partiallyFiltered).flt(fqn -> !fqn.equals("?")).arr();
        if (!filtered.has()) {
            return non();
        } else {
            PhpType.PhpTypeBuilder builder = PhpType.builder();
            for (String fqn: filtered) {
                if (fqn.startsWith("#S")) {
                    // since 2019.2.3 phpstorm started to prefix _static_ class type with "#S"
                    fqn = fqn.substring(2);
                }
                // #-#-#o#Э#A#M#C\DeepTest\ExactKeysUnitTest.provide_psalmFieldClosure.0?n?m - first argument of the said function
                // #-#-#M#C\App\Models\City.get?n?m|#-#-#?#M#C\App\Models\City.get?1?n?m|? - static
                Opt<L<String>> asMc = Tls.regex("[#\\-]*#M#C(.+?)\\.\\w+.*", fqn);
                if (asMc.has()) {
                    // I guess #M#C means "Member's class"
                    fqn = asMc.unw().get(0);
                }
                builder.add(fqn);
            }
            return som(builder.build());
        }
    }

    public static Set<String> ideaTypeToFqn(PhpType ideaType)
    {
        It<String> types = filterObjPst(ideaType)
            .fap(pst -> It(pst.getTypes()));
        return new HashSet<>(types.arr());
    }

    public static It<PhpClass> resolveIdeaTypeCls(PhpType ideaType, Project project)
    {
        return It(ideaTypeToFqn(ideaType))
            .fap(clsPath -> PhpIndex.getInstance(project).getAnyByFQN(clsPath))
            .fop(Lang::opt);
    }

    private static It<PhpClass> resolveIdeaTypeClsRelaxed(It<PhpType> ideaTypeTit, Project project)
    {
        MemIt<PhpType> ideaTypes = ideaTypeTit.mem();
        return It.frs(
            () -> ideaTypes.fap(tpe -> resolveIdeaTypeCls(tpe, project)),
            // allow to omit namespace in php doc class references
            () -> ideaTypes
                .fap(it -> ideaTypeToFqn(it))
                .fap(fqn -> MemRes.findClsByFqnPart(fqn, project))
        );
    }

    public static It<PhpClass> resolveMtInstCls(Mt mtArg, Project project)
    {
        It<PhpType> instTypes = opt(mtArg).fap(mt -> mt.getIdeaTypes());
        return resolveIdeaTypeClsRelaxed(instTypes, project);
    }

    public static It<PhpClass> resolveMtClsRefCls(Mt mtArg, Project project)
    {
        It<PhpType> clsRefTypes = mtArg.types.fap(t -> t.clsRefType);
        return resolveIdeaTypeClsRelaxed(clsRefTypes, project);
    }

    /** resolves class of both class instance types and class reference types */
    public static It<PhpClass> resolveMtCls(Mt mtArg, Project project)
    {
        return It.cnc(
            resolveMtInstCls(mtArg, project),
            resolveMtClsRefCls(mtArg, project)
        );
    }

    public It<PhpClass> resolveInstEpxrCls(PhpExpression expr)
    {
        Mt mt = Mt.mem(ctx.findExprType(expr));
        return resolveMtInstCls(mt, expr.getProject());
    }

    public It<PhpClass> resolveInstPsiCls(PsiElement instExpr)
    {
        return opt(instExpr.getFirstChild())
            .fop(toCast(PhpExpression.class))
            .fap(xpr -> resolveInstEpxrCls(xpr))
            ;
    }

    public static Opt<PhpClass> resolveClsRefPsiCls(PsiElement clsPsi)
    {
        return opt(clsPsi.getFirstChild())
            .fop(expr -> Opt.fst(
                () -> Tls.cast(ClassConstantReferenceImpl.class, expr)
                    .flt(cst -> Objects.equals(cst.getName(), "class"))
                    .map(cst -> cst.getClassReference())
                    .fop(toCast(ClassReferenceImpl.class))
                    .map(clsRef -> clsRef.resolve())
                    .fop(toCast(PhpClass.class)),
                () -> Tls.cast(StringLiteralExpression.class, expr)
                    .map(lit -> lit.getContents())
                    .fop(clsName -> Opt.fst(
                        () -> "self".equals(clsName)
                            ? Tls.findParent(clsPsi, PhpClass.class, a -> true)
                            : opt(null),
                        () -> It(PhpIndex.getInstance(expr.getProject())
                            .getAnyByFQN(clsName)).fst()
                    ))
            ));
    }

    /** like in [Ns\Employee::class, 'getSalary'] */
    private It<Method> resolveMethodFromArray(L<PsiElement> refParts)
    {
        return refParts.gat(1)
            .map(psi -> psi.getFirstChild())
            .fop(toCast(StringLiteralExpression.class))
            .map(lit -> lit.getContents())
            .fap(met -> refParts.gat(0)
                .fap(clsPsi -> It.cnc(
                    resolveClsRefPsiCls(clsPsi),
                    resolveInstPsiCls(clsPsi)
                ))
                .fop(cls -> opt(cls.findMethodByName(met))));
    }

    private static int getIndent(PsiElement psi)
    {
        return opt(psi.getPrevSibling())
            .cst(PsiWhiteSpaceImpl.class)
            .fop(ws -> Tls.regex("^(.*\\n|)(\\s*?)$", ws.getText()))
            .fop(ma -> ma.gat(1))
            .map(space -> space.length())
            .def(0);
    }

    private static L<String> getTopComments(ArrayHashElement hashEl)
    {
        L<String> comments = list();
        PsiElement prev = hashEl.getPrevSibling();
        int indent = getIndent(hashEl);
        while (prev != null) {
            boolean shouldSkip = prev instanceof PsiWhiteSpaceImpl;
            if (prev instanceof PsiCommentImpl && getIndent(prev) == indent) {
                comments.add(0, prev.getText());
            } else if (!shouldSkip) {
                break;
            }
            prev = prev.getPrevSibling();
        }
        return comments;
    }

    // probably it would be good to also support comments like this:
    // 'someKey' => 213, // some key 1 description line 1
    //                  // still key 1 description line 2
    // 'someKey2' => 213, //bla bla about key 2
    private static Opt<String> getSideComments(ArrayHashElement hashEl)
    {
        PsiElement next = hashEl.getNextSibling();
        while (next != null) {
            boolean shouldSkip = next instanceof PsiWhiteSpaceImpl || next.getText().equals(",");
            if (next instanceof PsiCommentImpl) {
                return som(((PsiCommentImpl)next).getText());
            } else if (!shouldSkip || next.getText().contains("\n")) {
                return non();
            }
            next = next.getNextSibling();
        }
        return non();
    }

    private static It<String> gatherSurroundingComments(ArrayHashElement hashEl)
    {
        return It.cnc(getTopComments(hashEl), getSideComments(hashEl))
            .map(c -> c.replaceAll("^/\\*\\s*([\\s\\S]+?)\\*/$", "$1"))
            .map(c -> c.replaceAll("^//\\s*", ""));
    }

    private L<Key> resolveAssoc(ArrayCreationExpressionImpl expr)
    {
        return It(expr.getHashElements()).fap((keyRec) -> opt(keyRec.getValue())
            .fop(toCast(PhpExpression.class))
            .fap(v -> {
                IIt<DeepType> valtit = ctx.findExprType(v);
                IReusableIt<DeepType> mit = valtit instanceof IResolvedIt ? valtit.arr() : valtit.mem();
                Granted<Mt> getType = Granted(new Mt(mit));
                L<String> comments = gatherSurroundingComments(keyRec).arr();
                return opt(keyRec.getKey())
                    .fop(toCast(PhpExpression.class))
                    .map(keyPsi -> ctx.findExprType(keyPsi))
                    .map(keyTypes -> keyTypes.fop(t -> opt(t.stringValue)))
                    .fap(keyStrValues -> {
                        if (keyStrValues.has()) {
                            return keyStrValues.map(key ->
                                new Key(key, ctx.getRealPsi(keyRec))
                                    .addType(getType, Tls.getIdeaType(v)));
                        } else {
                            Key keyEntry = new Key(
                                KeyType.unknown(keyRec), opt(keyRec.getKey()).def(expr)
                            );
                            return som(keyEntry.addType(getType));
                        }
                    })
                    .map(ke -> ke.addComments(comments));
            }))
            .arr();
    }

    public DeepType resolve(ArrayCreationExpressionImpl expr)
    {
        L<PsiElement> orderedParams = It(expr.getChildren())
            .flt(psi -> !(psi instanceof ArrayHashElement)).arr();

        It<Lang.F<IExprCtx, MemIt<DeepType>>> returnTypeGetters = resolveMethodFromArray(orderedParams)
            .map(meth -> MethCallRes.findMethRetType(meth))
            .map(retTypeGetter -> (ctx) -> new MemIt<>(retTypeGetter.apply(ctx)));

        L<Key> idxKeys = orderedParams
            .rap((valuePsi, i) -> opt(valuePsi.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .map(val -> new Key(i + "", ctx.getRealPsi(val))
                    .addType(() -> ctx.findExprType(val).wap(Mt::mem), Tls.getIdeaType(val))))
            .arr();

        L<Key> assocKeys = resolveAssoc(expr);

        return new Build(expr, PhpType.ARRAY)
            .keys(assocKeys.cct(idxKeys))
            .returnTypeGetters(returnTypeGetters)
            .get();
    }
}
