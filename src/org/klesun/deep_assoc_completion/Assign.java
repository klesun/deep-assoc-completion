package org.klesun.deep_assoc_completion;

import com.intellij.psi.PsiElement;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.List;

/**
 * contains info related to assignment to a
 * specific associative array variable key
 */
public class Assign extends Lang
{
    final public List<String> keys;
    // list?
    final public S<MultiType> assignedType;
    // when true, that means this assignment happens _always_,
    // i.e. it is not inside an "if" branch or a loop
    final public boolean didSurelyHappen;
    // where to look in GO TO navigation
    final public PsiElement psi;

    public Assign(List<String> keys, S<MultiType> assignedType, boolean didSurelyHappen, PsiElement psi)
    {
        this.keys = keys;
        this.assignedType = Tls.onDemand(assignedType);
        this.didSurelyHappen = didSurelyHappen;
        this.psi = psi;
    }
}
