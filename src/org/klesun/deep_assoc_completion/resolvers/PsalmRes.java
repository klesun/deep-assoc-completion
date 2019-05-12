package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.deep_assoc_completion.structures.psalm.IType;
import org.klesun.deep_assoc_completion.structures.psalm.PsalmFuncInfo;
import org.klesun.deep_assoc_completion.structures.psalm.TAssoc;
import org.klesun.deep_assoc_completion.structures.psalm.TClass;
import org.klesun.lang.It;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import static org.klesun.lang.Lang.*;

public class PsalmRes {
    final private IExprCtx ctx;

    public PsalmRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static It<DeepType> psalmToDeep(IType psalmType, PsiElement goToPsi)
    {
        return It.cnc(
            non()
            , Tls.cast(TAssoc.class, psalmType)
                .map(assoc -> Mkt.assoc(goToPsi, Lang.It(assoc.keys.entrySet()).map(e -> {
                    String keyName = e.getKey();
                    IType psalmVal = e.getValue();
                    It<DeepType> valTit = psalmToDeep(psalmVal, goToPsi);
                    return T2(keyName, new Mt(valTit));
                })))
            , Tls.cast(TClass.class, psalmType)
                .map(cls -> {
                    PhpType phpType = new PhpType().add(cls.fqn);
                    DeepType deep = new DeepType(goToPsi, phpType, false);
                    deep.generics = Lang.It(cls.generics)
                        .map(psalm -> psalmToDeep(psalm, goToPsi))
                        .map(tit -> new Mt(tit))
                        .arr();
                    // see https://psalm.dev/docs/templated_annotations/#builtin-templated-classes-and-interfaces
                    Boolean isArrayLike = false
                        || cls.fqn.equals("array") || cls.fqn.equals("\\array")
                        || cls.fqn.equals("iterable") || cls.fqn.equals("\\iterable")
                        || cls.fqn.equals("Traversable") || cls.fqn.equals("\\Traversable")
                        || cls.fqn.equals("ArrayAccess") || cls.fqn.equals("\\ArrayAccess")
                        || cls.fqn.equals("IteratorAggregate") || cls.fqn.equals("\\IteratorAggregate")
                        || cls.fqn.equals("Iterator") || cls.fqn.equals("\\Iterator")
                        || cls.fqn.equals("SeekableIterator") || cls.fqn.equals("\\SeekableIterator")
                        || cls.fqn.equals("Generator") || cls.fqn.equals("\\Generator")
                        || cls.fqn.equals("ArrayObject") || cls.fqn.equals("\\ArrayObject")
                        || cls.fqn.equals("ArrayIterator") || cls.fqn.equals("\\ArrayIterator")
                        || cls.fqn.equals("SplDoublyLinkedList") || cls.fqn.equals("\\SplDoublyLinkedList")
                        // these two probably should not be allowed to have 2 generics...
                        || cls.fqn.equals("DOMNodeList") || cls.fqn.equals("\\DOMNodeList")
                        || cls.fqn.equals("SplQueue") || cls.fqn.equals("\\SplQueue")
                        ;
                    if (isArrayLike) {
                        if (cls.generics.size() == 1) {
                            deep.addKey(KeyType.integer(goToPsi)).addType(() -> {
                                It<DeepType> tit = psalmToDeep(cls.generics.get(0), goToPsi);
                                return new Mt(tit);
                            });
                        } else if (cls.generics.size() == 2) {
                            It<DeepType> kit = psalmToDeep(cls.generics.get(1), goToPsi);
                            deep.addKey(KeyType.mt(kit, goToPsi)).addType(() -> {
                                It<DeepType> tit = psalmToDeep(cls.generics.get(1), goToPsi);
                                return new Mt(tit);
                            });
                        }
                    }
                    return deep;
                })
        );
    }

    public static It<DeepType> resolveReturn(PhpDocReturnTag docTag, IExprCtx funcCtx)
    {
        return opt(docTag.getParent())
            .cst(PhpDocComment.class)
            .fap(docComment -> PsalmFuncInfo.parse(docComment).returnType)
            .fap(psalmt -> psalmToDeep(psalmt, docTag));
    }

    public static It<DeepType> resolveVar(PhpDocComment docComment, String varName)
    {
        PsalmFuncInfo paslmInfo = PsalmFuncInfo.parse(docComment);
        return opt(paslmInfo.params.get(varName))
            .fap(psalmt -> psalmToDeep(psalmt, docComment));
    }
}
