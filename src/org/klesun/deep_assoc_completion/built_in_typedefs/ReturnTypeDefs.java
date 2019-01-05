package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import org.klesun.deep_assoc_completion.contexts.IFuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;

import static org.klesun.deep_assoc_completion.structures.Mkt.*;
import static org.klesun.lang.Lang.T2;
import static org.klesun.lang.Lang.list;

public class ReturnTypeDefs {

    public static DeepType curl_getinfo(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("url", str(call).mt()),
            T2("content_type", str(call).mt()),
            T2("http_code", inte(call).mt()),
            T2("header_size", inte(call).mt()),
            T2("request_size", inte(call).mt()),
            T2("filetime", inte(call).mt()),
            T2("ssl_verify_result", inte(call).mt()),
            T2("redirect_count", inte(call).mt()),
            T2("total_time", floate(call).mt()),
            T2("namelookup_time", floate(call).mt()),
            T2("connect_time", floate(call).mt()),
            T2("pretransfer_time", floate(call).mt()),
            T2("size_upload", floate(call).mt()),
            T2("size_download", floate(call).mt()),
            T2("speed_download", floate(call).mt()),
            T2("speed_upload", floate(call).mt()),
            T2("download_content_length", floate(call).mt()),
            T2("upload_content_length", floate(call).mt()),
            T2("starttransfer_time", floate(call).mt()),
            T2("redirect_time", floate(call).mt()),
            T2("certinfo", arr(call).mt()),
            T2("redirect_url", str(call).mt()),
            T2("primary_ip", str(call).mt()),
            T2("primary_port", inte(call).mt()),
            T2("local_ip", str(call).mt()),
            T2("local_port", inte(call).mt())
        ));
    }

    public static DeepType stream_get_meta_data(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("timed_out", bool(call).mt()),
            T2("blocked", bool(call).mt()),
            T2("eof", bool(call).mt()),
            T2("wrapper_type", str(call).mt()),
            T2("stream_type", str(call).mt()),
            T2("mode", str(call).mt()),
            T2("unread_bytes", inte(call).mt()),
            T2("seekable", bool(call).mt()),
            T2("uri", str(call).mt())
        ));
    }

    public static DeepType mysqli_get_links_stats(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("total", inte(call).mt()),
            T2("active_plinks", inte(call).mt()),
            T2("cached_plinks", inte(call).mt())
        ));
    }

    public static DeepType localeconv(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("decimal_point", str(call).mt()),
            T2("thousands_sep", str(call).mt()),
            T2("int_curr_symbol", str(call).mt()),
            T2("currency_symbol", str(call).mt()),
            T2("mon_decimal_point", str(call).mt()),
            T2("mon_thousands_sep", str(call).mt()),
            T2("positive_sign", str(call).mt()),
            T2("negative_sign", str(call).mt()),
            T2("int_frac_digits", inte(call).mt()),
            T2("frac_digits", inte(call).mt()),
            T2("p_cs_precedes", inte(call).mt()),
            T2("p_sep_by_space", inte(call).mt()),
            T2("n_cs_precedes", inte(call).mt()),
            T2("n_sep_by_space", inte(call).mt()),
            T2("p_sign_posn", inte(call).mt()),
            T2("n_sign_posn", inte(call).mt()),
            T2("grouping", arr(call).mt()),
            T2("mon_grouping", arr(call).mt())
        ));
    }

    public static DeepType proc_get_status(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("command", str(call).mt()),
            T2("pid", inte(call).mt()),
            T2("running", bool(call).mt()),
            T2("signaled", bool(call).mt()),
            T2("stopped", bool(call).mt()),
            T2("exitcode", inte(call).mt()),
            T2("termsig", inte(call).mt()),
            T2("stopsig", inte(call).mt())
        ));
    }

    public static DeepType getrusage(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("ru_oublock", inte(call).mt()),
            T2("ru_inblock", inte(call).mt()),
            T2("ru_msgsnd", inte(call).mt()),
            T2("ru_msgrcv", inte(call).mt()),
            T2("ru_maxrss", inte(call).mt()),
            T2("ru_ixrss", inte(call).mt()),
            T2("ru_idrss", inte(call).mt()),
            T2("ru_minflt", inte(call).mt()),
            T2("ru_majflt", inte(call).mt()),
            T2("ru_nsignals", inte(call).mt()),
            T2("ru_nvcsw", inte(call).mt()),
            T2("ru_nivcsw", inte(call).mt()),
            T2("ru_nswap", inte(call).mt()),
            T2("ru_utime.tv_usec", inte(call).mt()),
            T2("ru_utime.tv_sec", inte(call).mt()),
            T2("ru_stime.tv_usec", inte(call).mt()),
            T2("ru_stime.tv_sec", inte(call).mt())
        ));
    }

    public static DeepType error_get_last(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("type", inte(call).mt()),
            T2("message", str(call).mt()),
            T2("file", str(call).mt()),
            T2("line", inte(call).mt())
        ));
    }

    public static DeepType dns_get_record(FunctionReferenceImpl call)
    {
        DeepType assoct = assoc(call, list(
            T2("host", str(call).mt()),
            T2("class", str(call).mt()),
            T2("ttl", inte(call).mt()),
            T2("type", str(call).mt()),
            T2("mname", str(call).mt()),
            T2("rname", str(call).mt()),
            T2("serial", inte(call).mt()),
            T2("refresh", inte(call).mt()),
            T2("retry", inte(call).mt()),
            T2("expire", inte(call).mt()),
            T2("minimum-ttl", inte(call).mt()),
            T2("flags", inte(call).mt()),
            T2("tag", str(call).mt()),
            T2("value", str(call).mt())
        ));
        DeepType arrt = arr(call);
        arrt.addKey(KeyType.integer(call), call).addType(() -> new Mt(list(assoct)));
        return arrt;
    }

    public static DeepType fstat(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("dev", inte(call).mt()),
            T2("ino", inte(call).mt()),
            T2("mode", inte(call).mt()),
            T2("nlink", inte(call).mt()),
            T2("uid", inte(call).mt()),
            T2("gid", inte(call).mt()),
            T2("rdev", inte(call).mt()),
            T2("size", inte(call).mt()),
            T2("atime", inte(call).mt()),
            T2("mtime", inte(call).mt()),
            T2("ctime", inte(call).mt()),
            T2("blksize", inte(call).mt()),
            T2("blocks", inte(call).mt())
        ));
    }

    public static DeepType ob_get_status(IFuncCtx callCtx, FunctionReferenceImpl call)
    {
        DeepType assoct = assoc(call, list(
            T2("name", str(call).mt()),
            T2("type", inte(call).mt()),
            T2("flags", inte(call).mt()),
            T2("level", inte(call).mt()),
            T2("chunk_size", inte(call).mt()),
            T2("buffer_size", inte(call).mt()),
            T2("buffer_used", inte(call).mt())
        ));
        if (!callCtx.getArg(0).has()) {
            return assoct;
        } else {
            DeepType arrt = arr(call);
            arrt.addKey(KeyType.integer(call)).addType(() -> new Mt(list(assoct)));
            return arrt;
        }
    }

    public static DeepType getimagesize(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("0", inte(call).mt()),
            T2("1", inte(call).mt()),
            T2("2", inte(call).mt()),
            T2("3", str(call).mt()),
            T2("mime", str(call).mt()),
            T2("channels", inte(call).mt()),
            T2("bits", inte(call).mt())
        ));
    }

    public static DeepType parse_url(FunctionReferenceImpl call)
    {
        return assoc(call, list(
            T2("scheme", str(call).mt()),
            T2("host", str(call).mt()),
            T2("port", inte(call).mt()),
            T2("path", str(call).mt()),
            T2("fragment", str(call).mt()),
            T2("query", str(call).mt()),
            T2("user", str(call).mt()),
            T2("pass", str(call).mt())
        ));
    }
}
