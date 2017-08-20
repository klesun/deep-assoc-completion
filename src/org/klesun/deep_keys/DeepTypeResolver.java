package org.klesun.deep_keys;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_keys.helpers.FuncCtxNoArgs;
import org.klesun.deep_keys.resolvers.*;
import org.klesun.lang.Lang;
import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides mechanism to determine expression type.
 * Unlike original jetbrain's type resolver, this
 * includes associative array key information
 */
public class DeepTypeResolver extends Lang
{
    private static String dumpPsi(PsiElement psi, Integer level)
    {
        String result = "";
        result += "" + level + " - " + psi.getClass() + "\n";
        for (PsiElement subPsi: psi.getChildren()) {
            result += dumpPsi(subPsi, level + 1);
        }
        return result;
    }

    private static Opt<List<DeepType>> parseExpression(String expr, int depth, Project project)
    {
        expr = "<?php\n" + expr + ";";
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(PhpLanguage.INSTANCE, expr);

        return opt(psiFile.getFirstChild())
            .fap(toCast(GroupStatement.class))
            .map(gr -> gr.getFirstPsiChild())
            .fap(toCast(Statement.class))
            .map(st -> st.getFirstChild())
            .fap(toCast(PhpExpression.class))
            .map(ex -> findExprType(ex, depth));
    }

    public static Opt<List<DeepType>> parseDoc(String descr, int depth, Project project)
    {
        Pattern pattern = Pattern.compile("^\\s*=\\s*(.+)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(descr);
        if (matcher.matches()) {
            return parseExpression(matcher.group(1), depth, project);
        } else {
            return opt(null);
        }
    }

    private static List<PhpReturnImpl> findFunctionReturns(PsiElement func)
    {
        List<PhpReturnImpl> result = new ArrayList<>();
        for (PsiElement child: func.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpReturnImpl.class, child)
                .thn(result::add);

            findFunctionReturns(child).forEach(result::add);
        }
        return result;
    }

    public static List<DeepType> findFuncRetType(PsiElement meth, int depth)
    {
        List<DeepType> possibleTypes = list();

        findFunctionReturns(meth)
            .forEach(ret -> opt(ret.getArgument())
            .map(arg -> findExprType(arg, depth))
            .thn(possibleTypes::addAll));

        return possibleTypes;
    }

    public static L<DeepType> findExprType(PsiElement expr, int depth)
    {
        if (depth <= 0) {
            return list();
        }
        final int nextDepth = --depth;

        FuncCtxNoArgs ctx = new FuncCtxNoArgs(nextDepth);

        return Opt.fst(list(
            opt(null) // for coma formatting
            , Tls.cast(VariableImpl.class, expr)
                .map(v -> new VarRes(ctx).resolve(v))
            , Tls.cast(ArrayCreationExpressionImpl.class, expr)
                .map(arr -> list(new ArrCtorRes(ctx).resolve(arr)))
            , Tls.cast(FunctionReferenceImpl.class, expr)
                .map(call -> new NsFuncRes(ctx).resolve(call).types)
            , Tls.cast(MethodReferenceImpl.class, expr)
                .map(call -> new MethRes(ctx).resolveCall(call).types)
            , Tls.cast(ArrayAccessExpressionImpl.class, expr)
                .map(keyAccess -> new ArrAccRes(ctx).resolve(keyAccess).types)
            , Tls.cast(StringLiteralExpressionImpl.class, expr)
                .map(lit -> list(new DeepType(lit)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .map(v -> v.getFirstChild())
                .fap(toCast(FunctionImpl.class))
                .map(lambda -> list(new ClosRes(ctx).resolve(lambda)))
            , Tls.cast(PhpExpressionImpl.class, expr)
                .fap(casted -> opt(casted.getText())
                    .flt(text -> Tls.regex("^\\d+$", text).has())
                    .map(Integer::parseInt)
                    .map(num -> list(new DeepType(casted, num))))
            , Tls.cast(TernaryExpressionImpl.class, expr)
                .map(tern -> Stream.concat(
                    findExprType(tern.getTrueVariant(), nextDepth).stream(),
                    findExprType(tern.getFalseVariant(), nextDepth).stream()
                ).collect(Collectors.toList()))
            , Tls.cast(BinaryExpressionImpl.class, expr)
                // found this dealing with null coalescing, but
                // i suppose this rule will apply for all operators
                .map(bin -> Stream.concat(
                    findExprType(bin.getLeftOperand(), nextDepth).stream(),
                    findExprType(bin.getRightOperand(), nextDepth).stream()
                ).collect(Collectors.toList()))
            , Tls.cast(FieldReferenceImpl.class, expr)
                .map(fieldRef -> new FieldRes(ctx).resolve(fieldRef).types)
            , Tls.cast(PhpExpression.class, expr)
                .map(t -> list(new DeepType(t)))
//            , Tls.cast(ConstantReferenceImpl.class, expr)
//                .map(cnst -> list(new DeepType(cnst)))
        ))
            .map(types -> L(types))
            .els(() -> System.out.println("Unknown expression type " + expr.getText() + " " + expr.getClass()))
            .def(list());
    }

}
