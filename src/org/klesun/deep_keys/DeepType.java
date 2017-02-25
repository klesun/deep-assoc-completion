package org.klesun.deep_keys;

import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * contains info about associative
 * array key types among other things
 */
public class DeepType
{
    public final LinkedHashMap<String, List<DeepType>> keys = new LinkedHashMap<>();
    public final PhpTypedElement definition;
    public final PhpType briefType;

    public DeepType(PhpTypedElement definition) {
        this.definition = definition;
        this.briefType = definition.getType();
    }
}
