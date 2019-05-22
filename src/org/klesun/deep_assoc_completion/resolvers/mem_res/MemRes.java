package org.klesun.deep_assoc_completion.resolvers.mem_res;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.MemberReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.It;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

/**
 * should keep code reusable in both FieldRes and MethCallRes here
 */
public class MemRes {
    private IExprCtx ctx;

    public MemRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    public static It<PhpClass> findClsByFqnPart(String clsName, Project proj)
    {
        // allow to omit namespace in php doc class references
        PhpIndex idx = PhpIndex.getInstance(proj);
        if (clsName.isEmpty()) {
            return It.non();
        } else {
            String clean = clsName.replaceAll("^\\\\", "");
            return It.cnc(
                idx.getClassesByName(clean),
                idx.getInterfacesByName(clean),
                idx.getInterfacesByName(clean)
            ).flt(cls -> cls.getFQN().endsWith(clean));
        }
    }

    public It<PhpClass> resolveCls(MemberReference mem)
    {
        return opt(mem.getClassReference()).fap(obj -> {
            String cls = obj.getText();
            Project proj = mem.getProject();
            Opt<PhpClass> docCls = It.cnc(non()
                , ctx.getFakeFileSource() // phpdoc above class
                    .fap(tag -> Tls.getParents(tag).cct(som(tag)))
                    .cst(PhpDocComment.class)
                    .fap(doc -> Tls.getNextSiblings(doc))
                    .flt(sib -> !(sib instanceof PsiWhiteSpace))
                , ctx.getFakeFileSource() // phpdoc inside class
                    .fap(doc -> Tls.getParents(doc).cct(som(doc)))
            ).cst(PhpClass.class).fst();

            S<It<PhpClass>> docSelfCit = () -> docCls.fap(clsPsi -> {
                if (list("self", "static").contains(cls)) {
                    return ctx.getSelfType()
                        .fap(ideat -> ArrCtorRes.resolveIdeaTypeCls(ideat, proj))
                        .def(som(clsPsi));
                } else if ("$this".equals(cls)) {
                    Mt thisMt = ctx.getThisType().wap(Mt::new);
                    return ArrCtorRes.resolveMtInstCls(thisMt, proj)
                        .def(som(clsPsi));
                } else {
                    return It.non();
                }
            });

            S<It<PhpClass>> realCit = () -> It.frs(
                () -> ctx.getSelfType()
                    // IDEA resolves static:: incorrectly, it either treats it
                    // same as self::, either does not resolve it at all
                    .flt(typ -> obj.getText().equals("static"))
                    .fap(typ -> ArrCtorRes.resolveIdeaTypeCls(typ, obj.getProject())),
                () -> {
                    Mt mt = new Mt(ctx.findExprType(obj));
                    return mem.isStatic()
                        ? ArrCtorRes.resolveMtClsRefCls(mt, proj)
                        : ArrCtorRes.resolveMtInstCls(mt, proj);
                }
            );
            return It.frs(docSelfCit, realCit);
        }).unq();
    }
}
