package org.klesun.deep_assoc_completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.apache.commons.lang.StringEscapeUtils;
import org.klesun.deep_assoc_completion.DeepType;
import org.klesun.deep_assoc_completion.ScopeFinder;
import org.klesun.deep_assoc_completion.helpers.ArgOrder;
import org.klesun.deep_assoc_completion.helpers.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

import javax.annotation.Nullable;

public class FuncCallRes extends Lang
{
    private FuncCtx ctx;

    public FuncCallRes(FuncCtx ctx)
    {
        this.ctx = ctx;
    }

    private MultiType findPsiExprType(PsiElement psi)
    {
        return Tls.cast(PhpExpression.class, psi)
            .map(casted -> ctx.findExprType(casted))
            .def(new MultiType(L()));
    }

    private static L<VariableImpl> findVarRefsInFunc(GroupStatement meth, String varName)
    {
        return Tls.findChildren(
            meth, VariableImpl.class,
            subPsi -> !(subPsi instanceof FunctionImpl)
        ).flt(varUsage -> varName.equals(varUsage.getName()));
    }

    private static PhpType getDocType(Function func)
    {
        //return func.getDocType();
        return opt(func.getDocComment())
            .map(doc -> doc.getReturnTag())
            .map(ret -> ret.getType())
            .def(PhpType.EMPTY)
            ;
    }

    private L<DeepType> findBuiltInFuncCallType(FunctionReferenceImpl call)
    {
        return opt(call.getName()).map(name -> {
            L<DeepType> result = L();
            FuncCtx callCtx = ctx.subCtxDirect(call);
            PsiElement[] params = call.getParameters();
            L<PsiElement> lParams = L(params);
            if (name.equals("array_map")) {
                DeepType mapRetType = new DeepType(call);
                MultiType arrMt = callCtx.getArgMt(1);
                FuncCtx subCtx = L(params).gat(1)
                    .fop(array -> Tls.cast(PhpExpression.class, array))
                    .uni(argArr -> ctx.subCtxSingleArgArr(argArr),
                        () -> new FuncCtx(ctx.getSearch())
                    );
                S<MultiType> getElMt = () -> callCtx.getArgMt(0).types
                    .fap(t -> t.getReturnTypes(subCtx))
                    .wap(MultiType::new);
                arrMt.types.fch(t -> {
                    DeepType mapped = new DeepType(t.definition, PhpType.ARRAY);
                    t.keys.forEach((k, v) -> mapped.addKey(k, v.definition)
                        .addType(getElMt, call.getType().elementType()));
                    result.add(mapped);
                });
                if (arrMt.hasNumberIndexes()) {
                    mapRetType.listElTypes.add(getElMt);
                } else if (
                    arrMt.getKeyNames().size() == 0 ||
                    arrMt.types.any(t -> t.anyKeyElTypes.size() > 0)
                ) {
                    mapRetType.anyKeyElTypes.add(getElMt);
                }
                result.add(mapRetType);
            } else if (name.equals("array_filter") || name.equals("array_reverse")
                    || name.equals("array_splice") || name.equals("array_slice")
                    || name.equals("array_values")
            ) {
                // array type unchanged
                result.addAll(callCtx.getArgMt(0).types);
            } else if (name.equals("array_combine")) {
                DeepType combine = new DeepType(call, PhpType.ARRAY);
                Dict<L<DeepType>> keyToTypes = callCtx.getArgMt(0).getEl().types
                    .gop(t -> opt(t.stringValue));
                S<MultiType> getElMt = () -> callCtx.getArgMt(1).getEl();
                PhpType ideaElType = L(params).gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(e -> e.getType()).def(PhpType.MIXED);
                keyToTypes.fch((types,keyName) -> combine
                    .addKey(keyName, types.map(t -> t.definition).fst().def(call))
                        .addType(getElMt, ideaElType));
                combine.anyKeyElTypes.add(getElMt);
                result.add(combine);
            } else if (name.equals("array_pop") || name.equals("array_shift")
                    || name.equals("current") || name.equals("end") || name.equals("next")
                    || name.equals("prev") || name.equals("reset")
            ) {
                result.addAll(callCtx.getArgMt(0).getEl().types);
            } else if (name.equals("array_merge")) {
                for (int i = 0; i < params.length; ++i) {
                    result.addAll(callCtx.getArg(i).fap(mt -> mt.types));
                }
            } else if (name.equals("array_column")) {
                MultiType elType = callCtx.getArgMt(0).getEl();
                @Nullable String keyName = callCtx.getArgMt(1).getStringValue();
                DeepType type = new DeepType(call);
                type.listElTypes.add(() -> elType.getKey(keyName));
                result.add(type);
            } else if (name.equals("array_chunk")) {
                result.add(callCtx.getArgMt(0).getInArray(call));
            } else if (name.equals("array_intersect_key") || name.equals("array_diff_key")
                    || name.equals("array_intersect_assoc") || name.equals("array_diff_assoc")
            ) {
                // do something more clever?
                result.addAll(callCtx.getArgMt(0).types);
            } else if (name.equals("compact")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                Tls.findParent(call, GroupStatement.class, a -> true)
                    .thn(scope -> Tls.range(0, params.length)
                        .fch(i -> callCtx.getArg(i)
                            .map(keyt -> keyt.getStringValue())
                            .thn(varName -> {
                                L<VariableImpl> refs = findVarRefsInFunc(scope, varName)
                                    .flt(ref -> ScopeFinder.didPossiblyHappen(ref, call))
                                    ;
                                if (refs.size() > 0) {
                                    PhpType briefType = new PhpType();
                                    refs.map(var -> Tls.getIdeaType(var)).fch(briefType::add);
                                    arrt.addKey(varName, call)
                                        .addType(() -> refs
                                            .fap(ref -> ctx.findExprType(ref).types)
                                            .wap(MultiType::new), briefType);
                                }
                            })));
                result.add(arrt);
            } else if (name.equals("implode")) {
                String delim = callCtx.getArgMt(0).getStringValues().gat(0).def(" ");
                L<String> parts = callCtx.getArgMt(1).types
                    .fap(t -> L(t.keys.values()))
                    .fap(kv -> kv.getTypes())
                    .fop(t -> opt(t.stringValue));
                String joined = Tls.implode(delim, parts);
                // PHP string is not java string, of course, but pretty close
                String unescaped = StringEscapeUtils.unescapeJava(joined);
                DeepType type = new DeepType(call, PhpType.STRING, unescaped);
                result.add(type);
            } else if (name.equals("array_keys")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> callCtx.getArgMt(0).getKeyNames()
                    .map(keyName -> new DeepType(call, PhpType.STRING, keyName))
                    .wap(types -> new MultiType(types)));
                result.add(arrt);
            } else if (name.equals("func_get_args")) {
                result.addAll(ctx.getArg(new ArgOrder(0, true)).fap(a -> a.types));
            } else if (name.equals("func_get_arg")) {
                callCtx.getArg(0)
                    .fop(mt -> opt(mt.getStringValue()))
                    .flt(str -> Tls.isNum(str))
                    .map(str -> Integer.parseInt(str))
                    .fop(order -> ctx.getArg(order))
                    .thn(mt -> result.addAll(mt.types));
            } else if (name.equals("call_user_func_array")) {
                FuncCtx newCtx = lParams.gat(1)
                    .fop(toCast(PhpExpression.class))
                    .map(args -> ctx.subCtxIndirect(args))
                    .def(new FuncCtx(ctx.getSearch()));
                callCtx.getArgMt(0).types
                    .fap(t -> t.getReturnTypes(newCtx))
                    .fch(t -> result.add(t));
            } else if (name.equals("explode")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> new MultiType(list(new DeepType(call, PhpType.STRING))));
                result.add(arrt);
            } else if (name.equals("curl_getinfo") && !callCtx.getArg(1).has()) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("url", call);
                arrt.addKey("content_type", call);
                arrt.addKey("http_code", call);
                arrt.addKey("header_size", call);
                arrt.addKey("request_size", call);
                arrt.addKey("filetime", call);
                arrt.addKey("ssl_verify_result", call);
                arrt.addKey("redirect_count", call);
                arrt.addKey("total_time", call);
                arrt.addKey("namelookup_time", call);
                arrt.addKey("connect_time", call);
                arrt.addKey("pretransfer_time", call);
                arrt.addKey("size_upload", call);
                arrt.addKey("size_download", call);
                arrt.addKey("speed_download", call);
                arrt.addKey("speed_upload", call);
                arrt.addKey("download_content_length", call);
                arrt.addKey("upload_content_length", call);
                arrt.addKey("starttransfer_time", call);
                arrt.addKey("redirect_time", call);
                arrt.addKey("certinfo", call);
                arrt.addKey("redirect_url", call);
                result.add(arrt);
            } else if (name.equals("stream_get_meta_data")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("timed_out", call);
                arrt.addKey("blocked", call);
                arrt.addKey("eof", call);
                arrt.addKey("wrapper_type", call);
                arrt.addKey("stream_type", call);
                arrt.addKey("mode", call);
                arrt.addKey("unread_bytes", call);
                arrt.addKey("seekable", call);
                arrt.addKey("uri", call);
                result.add(arrt);
            } else if (name.equals("mysqli_get_links_stats")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("total", call);
                arrt.addKey("active_plinks", call);
                arrt.addKey("cached_plinks", call);
                result.add(arrt);
            } else if (name.equals("localeconv")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("decimal_point", call);
                arrt.addKey("thousands_sep", call);
                arrt.addKey("int_curr_symbol", call);
                arrt.addKey("currency_symbol", call);
                arrt.addKey("mon_decimal_point", call);
                arrt.addKey("mon_thousands_sep", call);
                arrt.addKey("positive_sign", call);
                arrt.addKey("negative_sign", call);
                arrt.addKey("int_frac_digits", call);
                arrt.addKey("frac_digits", call);
                arrt.addKey("p_cs_precedes", call);
                arrt.addKey("p_sep_by_space", call);
                arrt.addKey("n_cs_precedes", call);
                arrt.addKey("n_sep_by_space", call);
                arrt.addKey("p_sign_posn", call);
                arrt.addKey("n_sign_posn", call);
                arrt.addKey("grouping", call);
                arrt.addKey("mon_grouping", call);
                result.add(arrt);
            } else if (name.equals("proc_get_status")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("command", call);
                arrt.addKey("pid", call);
                arrt.addKey("running", call);
                arrt.addKey("signaled", call);
                arrt.addKey("stopped", call);
                arrt.addKey("exitcode", call);
                arrt.addKey("termsig", call);
                arrt.addKey("stopsig", call);
                result.add(arrt);
            } else if (name.equals("getrusage")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("ru_oublock", call);
                arrt.addKey("ru_inblock", call);
                arrt.addKey("ru_msgsnd", call);
                arrt.addKey("ru_msgrcv", call);
                arrt.addKey("ru_maxrss", call);
                arrt.addKey("ru_ixrss", call);
                arrt.addKey("ru_idrss", call);
                arrt.addKey("ru_minflt", call);
                arrt.addKey("ru_majflt", call);
                arrt.addKey("ru_nsignals", call);
                arrt.addKey("ru_nvcsw", call);
                arrt.addKey("ru_nivcsw", call);
                arrt.addKey("ru_nswap", call);
                arrt.addKey("ru_utime.tv_usec", call);
                arrt.addKey("ru_utime.tv_sec", call);
                arrt.addKey("ru_stime.tv_usec", call);
                arrt.addKey("ru_stime.tv_sec", call);
                result.add(arrt);
            } else if (name.equals("error_get_last")) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("type", call);
                arrt.addKey("message", call);
                arrt.addKey("file", call);
                arrt.addKey("line", call);
                result.add(arrt);
            } else if (name.equals("dns_get_record")) {
                DeepType assoct = new DeepType(call, PhpType.ARRAY);
                assoct.addKey("host", call);
                assoct.addKey("class", call);
                assoct.addKey("ttl", call);
                assoct.addKey("type", call);
                assoct.addKey("mname", call);
                assoct.addKey("rname", call);
                assoct.addKey("serial", call);
                assoct.addKey("refresh", call);
                assoct.addKey("retry", call);
                assoct.addKey("expire", call);
                assoct.addKey("minimum", call);
                assoct.addKey("flags", call);
                assoct.addKey("tag", call);
                assoct.addKey("value", call);
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> new MultiType(list(assoct)));
                result.add(arrt);
            } else if (list("stat", "fstat", "lstat").contains(name)) {
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.addKey("dev", call);
                arrt.addKey("ino", call);
                arrt.addKey("mode", call);
                arrt.addKey("nlink", call);
                arrt.addKey("uid", call);
                arrt.addKey("gid", call);
                arrt.addKey("rdev", call);
                arrt.addKey("size", call);
                arrt.addKey("atime", call);
                arrt.addKey("mtime", call);
                arrt.addKey("ctime", call);
                arrt.addKey("blksize", call);
                arrt.addKey("blocks", call);
                result.add(arrt);
            } else if (name.equals("ob_get_status")) {
                DeepType assoct = new DeepType(call, PhpType.ARRAY);
                assoct.addKey("name", call);
                assoct.addKey("type", call);
                assoct.addKey("flags", call);
                assoct.addKey("level", call);
                assoct.addKey("chunk_size", call);
                assoct.addKey("buffer_size", call);
                assoct.addKey("buffer_used", call);
                if (!callCtx.getArg(0).has()) {
                    result.add(assoct);
                } else {
                    DeepType arrt = new DeepType(call, PhpType.ARRAY);
                    arrt.listElTypes.add(() -> new MultiType(list(assoct)));
                    result.add(arrt);
                }
            } else {
                // try to get type info from standard_2.php
                opt(call.resolve())
                    .fop(toCast(Function.class))
                    .thn(func -> result.add(new DeepType(call, getDocType(func))))
                    ;
            }
            return result;
        }).def(list());
    }

    public MultiType resolve(FunctionReferenceImpl funcCall)
    {
        FuncCtx funcCtx = ctx.subCtxDirect(funcCall);
        return new MultiType(list(
            findBuiltInFuncCallType(funcCall),
            opt(funcCall.getFirstChild())
                .fop(toCast(PhpExpression.class))
                .map(funcVar -> ctx.findExprType(funcVar))
                .map(mt -> mt.types.fap(t -> t.getReturnTypes(funcCtx)))
                .def(L()),
            opt(funcCall.resolve())
                .fop(Tls.toCast(FunctionImpl.class))
                .map(func -> new ClosRes(ctx).resolve(func).getReturnTypes(funcCtx))
                .def(L())
        ).fap(a -> a));
    }
}
