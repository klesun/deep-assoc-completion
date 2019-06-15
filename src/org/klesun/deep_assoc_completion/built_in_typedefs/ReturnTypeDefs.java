package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.contexts.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.DirectTypeResolver;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.lang.It;
import org.klesun.lang.L;

import static org.klesun.deep_assoc_completion.structures.Mkt.*;
import static org.klesun.lang.Lang.*;

public class ReturnTypeDefs
{
    final private IExprCtx ctx;

    public ReturnTypeDefs(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    private static DeepType curl_getinfo(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("url", str(call, "http://google.com/").mt()),
            T2("content_type", str(call, "text/html; charset=UTF-8").mt()),
            T2("http_code", inte(call, 301).mt()),
            T2("header_size", inte(call, 321).mt()),
            T2("request_size", inte(call, 49).mt()),
            T2("filetime", inte(call, -1).mt()),
            T2("ssl_verify_result", inte(call, 0).mt()),
            T2("redirect_count", inte(call, 0).mt()),
            T2("total_time", floate(call, 0.060094000000000002).mt()),
            T2("namelookup_time", floate(call, 0.028378).mt()),
            T2("connect_time", floate(call, 0.038482000000000002).mt()),
            T2("pretransfer_time", floate(call, 0.038517999999999997).mt()),
            T2("size_upload", floate(call, 0.0).mt()),
            T2("size_download", floate(call, 219.0).mt()),
            T2("speed_download", floate(call, 3650.0).mt()),
            T2("speed_upload", floate(call, 0.0).mt()),
            T2("download_content_length", floate(call, 219.0).mt()),
            T2("upload_content_length", floate(call, -1.0).mt()),
            T2("starttransfer_time", floate(call, 0.060032000000000002).mt()),
            T2("redirect_time", floate(call, 0.0).mt()),
            T2("redirect_url", str(call, "http://www.google.com/").mt()),
            T2("primary_ip", str(call, "172.217.21.142").mt()),
            T2("certinfo", arr(call).mt()),
            T2("primary_port", inte(call, 80).mt()),
            T2("local_ip", str(call, "10.128.8.117").mt()),
            T2("local_port", inte(call, 57382).mt())
        ));
    }

    private static DeepType stream_get_meta_data(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("timed_out", bool(call, false).mt()),
            T2("blocked", bool(call, true).mt()),
            T2("eof", bool(call, false).mt()),
            T2("wrapper_type", str(call, "PHP").mt()),
            T2("stream_type", str(call, "STDIO").mt()),
            T2("mode", str(call, "r").mt()),
            T2("unread_bytes", inte(call, 0).mt()),
            T2("seekable", bool(call, true).mt()),
            T2("uri", str(call, "php://stdin").mt())
        ));
    }

    private static DeepType mysqli_get_links_stats(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("total", inte(call).mt()),
            T2("active_plinks", inte(call).mt()),
            T2("cached_plinks", inte(call).mt())
        ));
    }

    private static DeepType localeconv(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("decimal_point", str(call, ".").mt()),
            T2("thousands_sep", str(call, "").mt()),
            T2("int_curr_symbol", str(call, "").mt()),
            T2("currency_symbol", str(call, "").mt()),
            T2("mon_decimal_point", str(call, "").mt()),
            T2("mon_thousands_sep", str(call, "").mt()),
            T2("positive_sign", str(call, "").mt()),
            T2("negative_sign", str(call, "").mt()),
            T2("int_frac_digits", inte(call, 127).mt()),
            T2("frac_digits", inte(call, 127).mt()),
            T2("p_cs_precedes", inte(call, 127).mt()),
            T2("p_sep_by_space", inte(call, 127).mt()),
            T2("n_cs_precedes", inte(call, 127).mt()),
            T2("n_sep_by_space", inte(call, 127).mt()),
            T2("p_sign_posn", inte(call, 127).mt()),
            T2("n_sign_posn", inte(call, 127).mt()),
            T2("grouping", arr(call).mt()),
            T2("mon_grouping", arr(call).mt())
        ));
    }

    private static DeepType proc_get_status(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("command", str(call, "ls").mt()),
            T2("pid", inte(call, 29879).mt()),
            T2("running", bool(call, false).mt()),
            T2("signaled", bool(call, false).mt()),
            T2("stopped", bool(call, false).mt()),
            T2("exitcode", inte(call, 0).mt()),
            T2("termsig", inte(call, 0).mt()),
            T2("stopsig", inte(call, 0).mt())
        ));
    }

    private static DeepType getrusage(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("ru_oublock", inte(call, 528).mt()),
            T2("ru_inblock", inte(call, 0).mt()),
            T2("ru_msgsnd", inte(call, 0).mt()),
            T2("ru_msgrcv", inte(call, 0).mt()),
            T2("ru_maxrss", inte(call, 24176).mt()),
            T2("ru_ixrss", inte(call, 0).mt()),
            T2("ru_idrss", inte(call, 0).mt()),
            T2("ru_minflt", inte(call, 1650).mt()),
            T2("ru_majflt", inte(call, 0).mt()),
            T2("ru_nsignals", inte(call, 0).mt()),
            T2("ru_nvcsw", inte(call, 224).mt()),
            T2("ru_nivcsw", inte(call, 0).mt()),
            T2("ru_nswap", inte(call, 0).mt()),
            T2("ru_utime.tv_usec", inte(call, 52714).mt()),
            T2("ru_utime.tv_sec", inte(call, 0).mt()),
            T2("ru_stime.tv_usec", inte(call, 11714).mt()),
            T2("ru_stime.tv_sec", inte(call, 0).mt())
        ));
    }

    private DeepType error_get_last(FunctionReferenceImpl call)
    {
        PhpIndex idx = PhpIndex.getInstance(call.getProject());
        L<String> errCstNames = list(
            "E_ERROR", // 1
            "E_WARNING", // 2
            "E_PARSE", // 4
            "E_NOTICE", // 8
            "E_CORE_ERROR", // 16
            "E_CORE_WARNING", // 32
            "E_COMPILE_ERROR", // 64
            "E_COMPILE_WARNING", // 128
            "E_USER_ERROR", // 256
            "E_USER_WARNING", // 512
            "E_USER_NOTICE", // 1024
            "E_STRICT", // 2048
            "E_RECOVERABLE_ERROR", // 4096
            "E_DEPRECATED", // 8192
            "E_USER_DEPRECATED", // 16384
            "E_ALL" // 32767
        );
        It<DeepType> errCstTyppes = errCstNames.fap(nme -> It(idx.getConstantsByName(nme)))
            .fap(cstDef -> DirectTypeResolver.resolveConst(cstDef, ctx));
        return assoc(call, list(
            T2("type", new Mt(errCstTyppes)),
            T2("message", str(call, "proc_get_status() expects parameter 1 to be resource, null given").mt()),
            T2("file", str(call, "php shell code").mt()),
            T2("line", inte(call, 1).mt())
        ));
    }

    private static DeepType dns_get_record(FunctionReferenceImpl call)
    {
        DeepType assoct = assoc(call, list(
            T2("host", str(call, "google.com").mt()),
            T2("class", str(call, "IN").mt()),
            T2("ttl", inte(call, 60).mt()),
            T2("type", str(call, "SOA").mt()),
            T2("mname", str(call, "ns1.google.com").mt()),
            T2("rname", str(call, "dns-admin.google.com").mt()),
            T2("serial", inte(call, 210413966).mt()),
            T2("refresh", inte(call, 900).mt()),
            T2("retry", inte(call, 900).mt()),
            T2("expire", inte(call, 1800).mt()),
            T2("minimum-ttl", inte(call).mt()),
            T2("flags", inte(call, 0).mt()),
            T2("tag", str(call, "issue").mt()),
            T2("value", str(call, "pki.goog▒").mt())
        ));
        DeepType arrt = arr(call);
        arrt.addKey(KeyType.integer(call), call).addType(() -> new Mt(list(assoct)));
        return arrt;
    }

    private static DeepType fstat(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("dev", inte(call, 2049).mt()),
            T2("ino", inte(call, 13238274).mt()),
            T2("mode", inte(call, 16877).mt()),
            T2("nlink", inte(call, 30).mt()),
            T2("uid", inte(call, 1000).mt()),
            T2("gid", inte(call, 1000).mt()),
            T2("rdev", inte(call, 0).mt()),
            T2("size", inte(call, 4096).mt()),
            T2("atime", inte(call, 1535395568).mt()),
            T2("mtime", inte(call, 1535390333).mt()),
            T2("ctime", inte(call, 1535390333).mt()),
            T2("blksize", inte(call, 4096).mt()),
            T2("blocks", inte(call, 8).mt())
        ));
    }

    private static DeepType ob_get_status(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType assoct = assoc(call, list(
            T2("name", str(call, "default output handler").mt()),
            T2("type", inte(call, 0).mt()),
            T2("flags", inte(call, 20592).mt()),
            T2("level", inte(call, 0).mt()),
            T2("chunk_size", inte(call, 0).mt()),
            T2("buffer_size", inte(call, 16384).mt()),
            T2("buffer_used", inte(call, 1).mt())
        ));
        if (!callCtx.getArg(0).has()) {
            return assoct;
        } else {
            DeepType arrt = arr(call);
            arrt.addKey(KeyType.integer(call)).addType(() -> new Mt(list(assoct)));
            return arrt;
        }
    }

    private static DeepType getimagesize(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("0", inte(call, 200).mt()),
            T2("1", inte(call, 200).mt()),
            T2("2", inte(call, 3).mt()),
            T2("3", str(call, "width=\"200\" height=\"200\"").mt()),
            T2("bits", inte(call, 8).mt()),
            T2("mime", str(call, "image/png").mt()),
            T2("channels", inte(call).mt())
        ));
    }

    private static DeepType parse_url(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("scheme", str(call, "https").mt()),
            T2("host", str(call, "mail.google.com").mt()),
            T2("port", inte(call, 80).mt()),
            T2("path", str(call, "/mail/u/1/&onpage=40").mt()),
            T2("fragment", str(call, "inbox/FMfcgxvzKQhBBjPqwdDkmmrgBMGfHvjz?page=5").mt()),
            T2("query", str(call).mt()),
            T2("user", str(call).mt()),
            T2("pass", str(call).mt())
        ));
    }
    private It<DeepType> image_type_to_mime_type(FunctionReferenceImpl call)
    {
        return Cst.IMAGETYPE_.map(nme -> nme.b)
            .map(strVal -> str(call, strVal));
    }

    private It<DeepType> debug_backtrace(FunctionReferenceImpl call)
    {
        DeepType arrt = arr(call);
        arrt.addKey(KeyType.integer(call))
            .addType(() -> new Mt(list(assoc(call, list(
                T2("file", str(call, "/var/www/vendor/something/Model.php").mt()),
                T2("line", inte(call, 465).mt()),
                T2("function", str(call, "getParams").mt()),
                T2("class", str(call, "Something\\Model").mt()),
                T2("type", new Mt(list(str(call, "::"), str(call, "->")))),
                T2("args", arr(call).mt())
            )))));
        return It(som(arrt));
    }

    public Iterable<DeepType> getReturnType(FunctionReferenceImpl call, IFuncCtx callCtx)
    {
        String name = opt(call.getName()).def("");
        if (name.equals("curl_getinfo") && !callCtx.getArg(1).has()) {
            return list(curl_getinfo(call));
        } else if (name.equals("stream_get_meta_data")) {
            return list(stream_get_meta_data(call));
        } else if (name.equals("mysqli_get_links_stats")) {
            return list(mysqli_get_links_stats(call));
        } else if (name.equals("localeconv")) {
            return list(localeconv(call));
        } else if (name.equals("proc_get_status")) {
            return list(proc_get_status(call));
        } else if (name.equals("getrusage")) {
            return list(getrusage(call));
        } else if (name.equals("error_get_last")) {
            return list(error_get_last(call));
        } else if (name.equals("dns_get_record")) {
            return list(dns_get_record(call));
        } else if (list("stat", "fstat", "lstat").contains(name)) {
            return list(fstat(call));
        } else if (name.equals("ob_get_status")) {
            return list(ob_get_status(callCtx, call));
        } else if (name.equals("getimagesize")) {
            return list(getimagesize(call));
        } else if (name.equals("parse_url")) {
            return list(parse_url(call));
        } else if (name.equals("image_type_to_mime_type")) {
            return image_type_to_mime_type(call);
        } else if (name.equals("debug_backtrace")) {
            return debug_backtrace(call);
        } else if (name.equals("preg_last_error")) {
            return cst(ctx, list(
                "PREG_NO_ERROR",
                "PREG_INTERNAL_ERROR",
                "PREG_BACKTRACK_LIMIT_ERROR",
                "PREG_RECURSION_LIMIT_ERROR",
                "PREG_BAD_UTF8_ERROR",
                "PREG_BAD_UTF8_OFFSET_ERROR",
                "PREG_JIT_STACKLIMIT_ERROR"
            ));
        } else {
            return list();
        }
    }
}
