package org.klesun.deep_assoc_completion.go_to_decl_providers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpCaches;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MemberReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.Nullable;
import org.klesun.deep_assoc_completion.completion_providers.DeepKeysPvdr;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.SearchContext;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;

import java.util.Map;

/**
 * this is kind of fake Go To Decl handler, it should not
 * have any logic, I'll just use it to trigger the heavy AssocTypePvdr
 * which should be used only on explicit user action
 */
public class TriggerObjTypeGoToDecl extends Lang implements GotoDeclarationHandler
{
    private L<? extends PsiElement> resolveMember(PhpClass cls, String name)
    {
        return list(
            L(cls.getFields()).flt(f -> f.getName().equals(name)),
            L(cls.getMethods()).flt(m -> m.getName().equals(name))
        ).fap(a -> a);
    }

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        Project project = editor.getProject();
        if (project == null) {
            return new PsiElement[]{};
        }

        // gonna do a nasty thing - remove cache of object PSI if class was
        // not resolved and resolve type again with my Type Provider this time
        Map<PsiElement, PhpType> typeCache = PhpCaches.getInstance(project).TYPE_CACHE;

        L<? extends PsiElement> psiTargets = opt(psiElement)
            .map(leaf -> leaf.getParent())
            .fop(toCast(MemberReference.class))
            .fap(mem -> opt(mem.getClassReference())
                .fap(ref -> {
                    PhpType objType = ref.getType().filterMixed().filterUnknown().filterNull().filterPrimitives();
                    if (objType.isEmpty()) {
                        typeCache.remove(ref);
                        objType = ref.getType(); // triggering type provider again, with Deep this time
                    }
                    // does not work for some reason
//                    return L(mem.multiResolve(false))
//                        .fop(res -> opt(res.getElement()));

                    return L(objType.filter(PhpType.OBJECT).getTypes())
                        .fap(clsPath -> L(PhpIndex.getInstance(project).getClassesByFQN(clsPath)))
                        // TODO: distinguish properties and methods
                        .fap(cls -> resolveMember(cls, mem.getName()));
                }));

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext)
    {
        return null;
    }
}
