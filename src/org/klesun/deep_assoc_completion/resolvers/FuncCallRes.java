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
import org.klesun.lang.Dict;
import org.klesun.lang.L;
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

    private static DeepType makeAssoc(PsiElement psi, L<T2<String, PhpType>> keys)
    {
        DeepType assoct = new DeepType(psi, PhpType.ARRAY);
        for (T2<String, PhpType> key: keys) {
            DeepType deepType = new DeepType(psi, key.b);
            MultiType mt = new MultiType(list(deepType));
            assoct.addKey(key.a, psi).addType(() -> mt, key.b);
        }
        return assoct;
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
                arrt.listElTypes.add(() -> callCtx.getArgMt(0).types
                    .fap(t -> L(t.keys.values()))
                    .map(k -> new DeepType(k.definition, PhpType.STRING, k.name))
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
                result.add(makeAssoc(call, list(
                    T2("url", PhpType.STRING),
                    T2("content_type", PhpType.STRING),
                    T2("http_code", PhpType.INT),
                    T2("header_size", PhpType.INT),
                    T2("request_size", PhpType.INT),
                    T2("filetime", PhpType.INT),
                    T2("ssl_verify_result", PhpType.INT),
                    T2("redirect_count", PhpType.INT),
                    T2("total_time", PhpType.FLOAT),
                    T2("namelookup_time", PhpType.FLOAT),
                    T2("connect_time", PhpType.FLOAT),
                    T2("pretransfer_time", PhpType.FLOAT),
                    T2("size_upload", PhpType.FLOAT),
                    T2("size_download", PhpType.FLOAT),
                    T2("speed_download", PhpType.FLOAT),
                    T2("speed_upload", PhpType.FLOAT),
                    T2("download_content_length", PhpType.FLOAT),
                    T2("upload_content_length", PhpType.FLOAT),
                    T2("starttransfer_time", PhpType.FLOAT),
                    T2("redirect_time", PhpType.FLOAT),
                    T2("certinfo", PhpType.ARRAY),
                    T2("redirect_url", PhpType.STRING),
                    T2("primary_ip", PhpType.STRING),
                    T2("primary_port", PhpType.INT),
                    T2("local_ip", PhpType.STRING),
                    T2("local_port", PhpType.INT)
                )));
            } else if (name.equals("stream_get_meta_data")) {
                result.add(makeAssoc(call, list(
                    T2("timed_out", PhpType.BOOLEAN),
                    T2("blocked", PhpType.BOOLEAN),
                    T2("eof", PhpType.BOOLEAN),
                    T2("wrapper_type", PhpType.STRING),
                    T2("stream_type", PhpType.STRING),
                    T2("mode", PhpType.STRING),
                    T2("unread_bytes", PhpType.INT),
                    T2("seekable", PhpType.BOOLEAN),
                    T2("uri", PhpType.STRING)
                )));
            } else if (name.equals("mysqli_get_links_stats")) {
                result.add(makeAssoc(call, list(
                    T2("total", PhpType.INT),
                    T2("active_plinks", PhpType.INT),
                    T2("cached_plinks", PhpType.INT)
                )));
            } else if (name.equals("localeconv")) {
                result.add(makeAssoc(call, list(
                    T2("decimal_point", PhpType.STRING),
                    T2("thousands_sep", PhpType.STRING),
                    T2("int_curr_symbol", PhpType.STRING),
                    T2("currency_symbol", PhpType.STRING),
                    T2("mon_decimal_point", PhpType.STRING),
                    T2("mon_thousands_sep", PhpType.STRING),
                    T2("positive_sign", PhpType.STRING),
                    T2("negative_sign", PhpType.STRING),
                    T2("int_frac_digits", PhpType.INT),
                    T2("frac_digits", PhpType.INT),
                    T2("p_cs_precedes", PhpType.INT),
                    T2("p_sep_by_space", PhpType.INT),
                    T2("n_cs_precedes", PhpType.INT),
                    T2("n_sep_by_space", PhpType.INT),
                    T2("p_sign_posn", PhpType.INT),
                    T2("n_sign_posn", PhpType.INT),
                    T2("grouping", PhpType.ARRAY),
                    T2("mon_grouping", PhpType.ARRAY)
                )));
            } else if (name.equals("proc_get_status")) {
                result.add(makeAssoc(call, list(
                    T2("command", PhpType.STRING),
                    T2("pid", PhpType.INT),
                    T2("running", PhpType.BOOLEAN),
                    T2("signaled", PhpType.BOOLEAN),
                    T2("stopped", PhpType.BOOLEAN),
                    T2("exitcode", PhpType.INT),
                    T2("termsig", PhpType.INT),
                    T2("stopsig", PhpType.INT)
                )));
            } else if (name.equals("getrusage")) {
                result.add(makeAssoc(call, list(
                    T2("ru_oublock", PhpType.INT),
                    T2("ru_inblock", PhpType.INT),
                    T2("ru_msgsnd", PhpType.INT),
                    T2("ru_msgrcv", PhpType.INT),
                    T2("ru_maxrss", PhpType.INT),
                    T2("ru_ixrss", PhpType.INT),
                    T2("ru_idrss", PhpType.INT),
                    T2("ru_minflt", PhpType.INT),
                    T2("ru_majflt", PhpType.INT),
                    T2("ru_nsignals", PhpType.INT),
                    T2("ru_nvcsw", PhpType.INT),
                    T2("ru_nivcsw", PhpType.INT),
                    T2("ru_nswap", PhpType.INT),
                    T2("ru_utime.tv_usec", PhpType.INT),
                    T2("ru_utime.tv_sec", PhpType.INT),
                    T2("ru_stime.tv_usec", PhpType.INT),
                    T2("ru_stime.tv_sec", PhpType.INT)
                )));
            } else if (name.equals("error_get_last")) {
                result.add(makeAssoc(call, list(
                    T2("type", PhpType.INT),
                    T2("message", PhpType.STRING),
                    T2("file", PhpType.STRING),
                    T2("line", PhpType.INT)
                )));
            } else if (name.equals("dns_get_record")) {
                DeepType assoct = makeAssoc(call, list(
                    T2("host", PhpType.STRING),
                    T2("class", PhpType.STRING),
                    T2("ttl", PhpType.INT),
                    T2("type", PhpType.STRING),
                    T2("mname", PhpType.STRING),
                    T2("rname", PhpType.STRING),
                    T2("serial", PhpType.INT),
                    T2("refresh", PhpType.INT),
                    T2("retry", PhpType.INT),
                    T2("expire", PhpType.INT),
                    T2("minimum-ttl", PhpType.INT),
                    T2("flags", PhpType.INT),
                    T2("tag", PhpType.STRING),
                    T2("value", PhpType.STRING)
                ));
                DeepType arrt = new DeepType(call, PhpType.ARRAY);
                arrt.listElTypes.add(() -> new MultiType(list(assoct)));
                result.add(arrt);
            } else if (list("stat", "fstat", "lstat").contains(name)) {
                result.add(makeAssoc(call, list(
                    T2("dev", PhpType.INT),
                    T2("ino", PhpType.INT),
                    T2("mode", PhpType.INT),
                    T2("nlink", PhpType.INT),
                    T2("uid", PhpType.INT),
                    T2("gid", PhpType.INT),
                    T2("rdev", PhpType.INT),
                    T2("size", PhpType.INT),
                    T2("atime", PhpType.INT),
                    T2("mtime", PhpType.INT),
                    T2("ctime", PhpType.INT),
                    T2("blksize", PhpType.INT),
                    T2("blocks", PhpType.INT)
                )));
            } else if (name.equals("ob_get_status")) {
                DeepType assoct = makeAssoc(call, list(
                    T2("name", PhpType.STRING),
                    T2("type", PhpType.INT),
                    T2("flags", PhpType.INT),
                    T2("level", PhpType.INT),
                    T2("chunk_size", PhpType.INT),
                    T2("buffer_size", PhpType.INT),
                    T2("buffer_used", PhpType.INT)
                ));
                if (!callCtx.getArg(0).has()) {
                    result.add(assoct);
                } else {
                    DeepType arrt = new DeepType(call, PhpType.ARRAY);
                    arrt.listElTypes.add(() -> new MultiType(list(assoct)));
                    result.add(arrt);
                }
            } else if (name.equals("getimagesize")) {
                result.add(makeAssoc(call, list(
                    T2("0", PhpType.INT),
                    T2("1", PhpType.INT),
                    T2("2", PhpType.INT),
                    T2("3", PhpType.STRING),
                    T2("mime", PhpType.STRING),
                    T2("channels", PhpType.INT),
                    T2("bits", PhpType.INT)
                )));
            } else if (name.equals("parse_url")) {
                result.add(makeAssoc(call, list(
                    T2("scheme", PhpType.STRING),
                    T2("host", PhpType.STRING),
                    T2("port", PhpType.INT),
                    T2("path", PhpType.STRING),
                    T2("fragment", PhpType.STRING),
                    T2("query", PhpType.STRING),
                    T2("user", PhpType.STRING),
                    T2("pass", PhpType.STRING)
                )));
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
