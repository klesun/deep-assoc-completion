package org.klesun.deep_assoc_completion.resolvers.other_plugin_integration;

import com.intellij.psi.PsiElement;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;

import java.util.HashMap;
import java.util.Map;

import static org.klesun.lang.Lang.F2;
import static org.klesun.lang.Lang.It;

public class DeepAssocApi
{
    private static DeepAssocApi inst;

    private Map<String, F2<String, PsiElement, Iterable<DeepType>>> customDocParsers = new HashMap<>();

    public static DeepAssocApi inst()
    {
        if (inst == null) {
            inst = new DeepAssocApi();
        }
        return inst;
    }

    /**
     * adding this primarily for deep-js-completion
     * plugin so that you could reference js vars in phpdoc
     */
    public void addCustomDocParser(String pluginKey, F2<String, PsiElement, Iterable<DeepType>> parser)
    {
        customDocParsers.put(pluginKey, parser);
    }

    public It<DeepType> parseDoc(String content, PsiElement psi)
    {
        if (content.matches("\\s*=\\s*\\[\\s*['\"]\\w*['\"]\\s*=>.*")) {
            // do not parse ['key' => 123] as javascript array
            return It.non();
        }
        return It(customDocParsers.entrySet())
            .fap(e -> {
                try {
                    return e.getValue().apply(content, psi);
                } catch (Throwable exc) {
                    System.out.println("deep-assoc plugin integration: got exception on registered " +
                        e.getKey() + " doc type info provider - " + exc.getClass() + " " + exc.getMessage());
                    return It.non();
                }
            });
    }
}
