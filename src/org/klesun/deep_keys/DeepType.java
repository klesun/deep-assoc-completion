package org.klesun.deep_keys;

import com.google.gson.annotations.Expose;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * contains info about associative
 * array key types among other things
 */
class DeepType
{
    // keys and types of associative array
    final LinkedHashMap<String, List<DeepType>> keys = new LinkedHashMap<>();
    // possible types of list element
    final List<DeepType> indexTypes = new ArrayList<>();
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    final List<DeepType> returnTypes = new ArrayList<>();
    final PsiElement definition;
    final PhpType briefType;

    DeepType(PhpExpression definition) {
        this(definition, definition.getType());
    }

    DeepType(PsiElement definition, PhpType briefType) {
        this.definition = definition;
        this.briefType = briefType;
    }
}
