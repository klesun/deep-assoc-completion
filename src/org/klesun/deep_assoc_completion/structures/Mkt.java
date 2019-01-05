package org.klesun.deep_assoc_completion.structures;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.lang.Lang;

/** short for "Make Type" */
public class Mkt {
    public static DeepType str(PsiElement psi, String content)
    {
        return new DeepType(psi, PhpType.STRING, content);
    }

    public static DeepType str(PsiElement psi)
    {
        return new DeepType(psi, PhpType.STRING);
    }

    public static DeepType inte(PsiElement psi)
    {
        return new DeepType(psi, PhpType.INT);
    }

    public static DeepType floate(PsiElement psi)
    {
        return new DeepType(psi, PhpType.FLOAT);
    }

    public static DeepType bool(PsiElement psi)
    {
        return new DeepType(psi, PhpType.BOOLEAN);
    }

    public static DeepType arr(PsiElement psi)
    {
        return new DeepType(psi, PhpType.ARRAY);
    }

    public static DeepType assoc(PsiElement psi, Iterable<Lang.T2<String, Mt>> keys)
    {
        DeepType assoct = new DeepType(psi, PhpType.ARRAY);
        for (Lang.T2<String, Mt> key: keys) {
            PhpType ideaType = key.b.getIdeaTypes().fst().def(PhpType.UNSET);
            assoct.addKey(key.a, psi).addType(() -> key.b, ideaType);
        }
        return assoct;
    }

    public static DeepType mixed(PsiElement psi)
    {
        return new DeepType(psi, PhpType.MIXED);
    }
}
