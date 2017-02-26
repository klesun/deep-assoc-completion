package org.klesun.deep_keys;

import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
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
    final PhpTypedElement definition;
    final PhpType briefType;

    DeepType(PhpTypedElement definition) {
        this.definition = definition;
        this.briefType = definition.getType();
    }
}
