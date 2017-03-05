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
public class DeepType
{
    // keys and types of associative array
    public final LinkedHashMap<String, Key> keys = new LinkedHashMap<>();
    // possible types of list element
    public final List<DeepType> indexTypes = new ArrayList<>();
    // applicable to closures and function names
    // (starting with self::) and [$obj, 'functionName'] tuples
    public final List<DeepType> returnTypes = new ArrayList<>();
    public final PsiElement definition;
    public final PhpType briefType;
    DeepType self = this;

    DeepType(PhpExpression definition) {
        this(definition, definition.getType());
    }

    DeepType(PsiElement definition, PhpType briefType) {
        this.definition = definition;
        this.briefType = briefType;
    }

    public Key addKey(String name, PsiElement definition)
    {
        Key keyEntry = new Key(name, definition);
        keys.put(keyEntry.name, keyEntry);
        return keyEntry;
    }

    public static class Key
    {
        final public String name;
        final public List<DeepType> types = new ArrayList<>();
        // where Go To Definition will lead
        final public PsiElement definition;

        private Key(String name, PsiElement definition)
        {
            this.name = name;
            this.definition = definition;
        }
    }
}
