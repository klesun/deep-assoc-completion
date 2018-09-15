package org.klesun.deep_assoc_completion;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.KeyType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import java.util.List;

/**
 * contains info related to assignment to a
 * specific associative array variable key
 */
public class Assign extends Lang
{
    final public List<KeyType> keys;
    // list?
    final public S<Mt> assignedType;
    // when true, that means this assignment happens _always_,
    // i.e. it is not inside an "if" branch or a loop
    final public boolean didSurelyHappen;
    // where to look in GO TO navigation
    final public PsiElement psi;
    final public PhpType briefType;

    public Assign(List<KeyType> keys, S<Mt> assignedType, boolean didSurelyHappen, PsiElement psi, PhpType briefType)
    {
        this.keys = keys;
        this.assignedType = Tls.onDemand(assignedType);
        this.didSurelyHappen = didSurelyHappen;
        this.psi = psi;
        this.briefType = briefType;
    }
}
