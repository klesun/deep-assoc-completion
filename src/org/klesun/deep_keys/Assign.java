package org.klesun.deep_keys;

import com.intellij.psi.PsiElement;

import java.util.List;

/**
 * contains info related to assignment to a
 * specific associative array variable key
 */
public class Assign
{
    final public List<String> keys;
    // list?
    final public List<DeepType> assignedType;
    // when true, that means this assignment happens _always_,
    // i.e. it is not inside an "if" branch or a loop
    final public boolean didSurelyHappen;
    // where to look in GO TO navigation
    final public PsiElement psi;

    public Assign(List<String> keys, List<DeepType> assignedType, boolean didSurelyHappen, PsiElement psi)
    {
        this.keys = keys;
        this.assignedType = assignedType;
        this.didSurelyHappen = didSurelyHappen;
        this.psi = psi;
    }
}
