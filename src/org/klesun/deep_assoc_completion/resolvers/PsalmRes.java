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
import org.klesun.deep_assoc_completion.structures.psalm.*;
import org.klesun.lang.It;
import org.klesun.lang.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.klesun.lang.Lang.*;

public class PsalmRes {
    final private IExprCtx ctx;

    public PsalmRes(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    //====================================================
    // following functions retrieve resulting type from signatures and resolved generics
    //====================================================

    private static boolean isArrayLike(TClass cls)
    {
        // see https://psalm.dev/docs/templated_annotations/#builtin-templated-classes-and-interfaces
        return cls.fqn.equals("array") || cls.fqn.equals("\\array")
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
            || cls.fqn.equals("SplQueue") || cls.fqn.equals("\\SplQueue");
    }

    private static It<DeepType.Key> genericsToArrKeys(List<IType> defs, PsiElement goToPsi, Map<String, MemIt<DeepType>> generics)
    {
        if (defs.size() == 1) {
            KeyType keyt = KeyType.integer(goToPsi);
            DeepType.Key keyObj = new DeepType.Key(keyt, goToPsi);
            keyObj.addType(() -> {
                It<DeepType> tit = psalmToDeep(defs.get(0), goToPsi, generics);
                return new Mt(tit);
            });
            return It(som(keyObj));
        } else if (defs.size() == 2) {
            It<DeepType> kit = psalmToDeep(defs.get(1), goToPsi, generics);
            KeyType keyt = KeyType.mt(kit, goToPsi);
            DeepType.Key keyObj = new DeepType.Key(keyt, goToPsi);
            keyObj.addType(() -> {
                It<DeepType> tit = psalmToDeep(defs.get(1), goToPsi, generics);
                return new Mt(tit);
            });
            return It(som(keyObj));
        } else {
            return It.non();
        }
    }

    private static It<DeepType> psalmClsToDeep(TClass cls, PsiElement goToPsi, Map<String, MemIt<DeepType>> generics)
    {
        Opt<MemIt<DeepType>> genOpt = opt(generics.get(cls.fqn));
        if (genOpt.has()) {
            return genOpt.fap(a -> a);
        } else {
            PhpType phpType = new PhpType().add(cls.fqn);
            DeepType deep = new DeepType(goToPsi, phpType, false);
            deep.generics = Lang.It(cls.generics)
                .map(psalm -> psalmToDeep(psalm, goToPsi, generics))
                .map(tit -> new Mt(tit))
                .arr();
            if (isArrayLike(cls)) {
                genericsToArrKeys(cls.generics, goToPsi, generics)
                    .fch(k -> deep.addKey(k));
            }
            return It(som(deep));
        }
    }

    private static It<DeepType> psalmToDeep(IType psalmType, PsiElement goToPsi, Map<String, MemIt<DeepType>> generics)
    {
        return It.cnc(
            non()
            , Tls.cast(TAssoc.class, psalmType)
                .map(assoc -> Mkt.assoc(goToPsi, Lang.It(assoc.keys.entrySet()).map(e -> {
                    String keyName = e.getKey();
                    IType psalmVal = e.getValue();
                    It<DeepType> valTit = psalmToDeep(psalmVal, goToPsi, generics);
                    return T2(keyName, new Mt(valTit));
                })))
            , Tls.cast(TClass.class, psalmType)
                .fap(cls -> psalmClsToDeep(cls, goToPsi, generics))
            // if we ever support references, should add check for infinite recursion here...
            , Tls.cast(TMulti.class, psalmType)
                .fap(multi -> It(multi.types)
                    .fap(t -> psalmToDeep(t, goToPsi, generics)))
            , Tls.cast(TPrimitive.class, psalmType)
                .map(prim -> new DeepType(goToPsi, prim.kind, prim.stringValue))
        );
    }

    //====================================================
    // following functions retrieve generic type from signatures and context
    //====================================================

    private static It<DeepType> getGenericTypeFromArg(IType psalmt, Mt deept, String generic, PsiElement psi)
    {
        return It.cnc(
            non()
            , Tls.cast(TClass.class, psalmt).fap(cls -> {
                PhpType phpType = new PhpType().add(cls.fqn);
                if (cls.fqn.equals(generic)) {
                    return deept.types;
                } else if (isArrayLike(cls)) {
                    if (cls.generics.size() == 1) {
                        Mt elMt = deept.getEl();
                        IType elPsalmt = cls.generics.get(0);
                        return getGenericTypeFromArg(elPsalmt, elMt, generic, psi);
                    } else if (cls.generics.size() == 2) {
                        Mt keyMt = deept.types.fap(t -> t.keys).fap(k -> k.keyType.getTypes).wap(Mt::new);
                        IType keyPsalmt = cls.generics.get(1);
                        It<DeepType> genKeyTit = getGenericTypeFromArg(keyPsalmt, keyMt, generic, psi);

                        Mt valMt = deept.getEl();
                        IType valPsalmt = cls.generics.get(1);
                        It<DeepType> getValTit = getGenericTypeFromArg(valPsalmt, valMt, generic, psi);

                        return It.cnc(genKeyTit, getValTit);
                    }
                } else {
                    // TODO: support keyed arrays, fields, generics from
                    //  class constructor, function types with generics
                }
                DeepType asParamCls = new DeepType(psi, phpType, false);
                asParamCls.generics = It(cls.generics)
                    .map(psalmgt -> deept.types
                        .fap(t -> t.generics)
                        .fap(gmt -> getGenericTypeFromArg(psalmgt, gmt, generic, psi))
                        .wap(Mt::new))
                    .arr();
                return It(som(asParamCls));
            })
        );
    }

    public static It<DeepType> getGenTypeFromFunc(PsalmFuncInfo.GenericDef g, L<PsalmFuncInfo.ArgDef> params, IExprCtx ctx, PsiElement psi)
    {
        return params.fap(p -> p.order.fap(o -> ctx.func().getArg(o))
            .fap(mt -> p.psalmType
                .fap(psalmt -> getGenericTypeFromArg(
                    psalmt, mt, g.name, psi
                ))));
    }

    private static Map<String, MemIt<DeepType>> getGenericTypes(PsalmFuncInfo psalmInfo, IExprCtx ctx)
    {
        Map<String, MemIt<DeepType>> result = new HashMap<>();
        psalmInfo.funcGenerics.fch(g -> {
            MemIt<DeepType> mit = getGenTypeFromFunc(
                g, psalmInfo.params, ctx, psalmInfo.psi
            ).mem();
            result.put(g.name, mit);
        });
        psalmInfo.classGenerics.fch((g, i) -> result.put(g.name, ctx.getThisType()
            .fap(t -> t.generics.gat(i))
            .fap(mt -> mt.types).mem()));
        return result;
    }

    //====================================================
    // following functions are entry points
    //====================================================

    public static It<DeepType> resolveReturn(PhpDocReturnTag docTag, IExprCtx ctx)
    {
        return opt(docTag.getParent())
            .cst(PhpDocComment.class)
            .map(docComment -> PsalmFuncInfo.parse(docComment))
            .fap(psalmInfo -> psalmInfo.returnType
                .fap(psalmt -> {
                    Map<String, MemIt<DeepType>> gents = getGenericTypes(psalmInfo, ctx);
                    return psalmToDeep(psalmt, docTag, gents);
                }));
    }

    public static It<DeepType> resolveVar(PhpDocComment docComment, String varName, IExprCtx ctx)
    {
        PsalmFuncInfo psalmInfo = PsalmFuncInfo.parse(docComment);
        Map<String, MemIt<DeepType>> generics = getGenericTypes(psalmInfo, ctx);
        return psalmInfo.params.flt(p -> p.name.equals(varName) || p.name.equals(""))
            .fap(p -> p.psalmType)
            .fap(psalmt -> psalmToDeep(psalmt, docComment, generics));
    }
}
