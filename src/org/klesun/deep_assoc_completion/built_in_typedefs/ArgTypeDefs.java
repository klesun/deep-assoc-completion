package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.structures.KeyType;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.lang.It;
import org.klesun.lang.Tls;

import static org.klesun.deep_assoc_completion.structures.Mkt.*;
import static org.klesun.lang.Lang.*;

public class ArgTypeDefs
{
    final private IExprCtx ctx;

    public ArgTypeDefs(IExprCtx ctx)
    {
        this.ctx = ctx;
    }

    // first arg
    private static DeepType stream_context_create(Function def)
    {
        return assoc(def, list(
            T2("http", assoc(def, list(
                T2("header", str(def, "Content-type: application/x-www-form-urlencoded\\r\\n").mt()),
                T2("method", new Mt(list("GET", "POST", "OPTIONS", "PUT", "HEAD", "DELETE", "CONNECT", "TRACE", "PATCH").map(m -> str(def, m)))),
                T2("content", str(def, "name=Vasya&age=26&price=400").mt()),
                T2("user_agent", str(def, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/71.0.3578.80 Chrome/71.0.3578.80 Safari/537.36").mt()),
                T2("proxy", str(def, "tcp://proxy.example.com:5100").mt()),
                T2("request_fulluri", bool(def).mt()),
                T2("follow_location", inte(def).mt()),
                T2("max_redirects", inte(def).mt()),
                T2("protocol_version", floate(def).mt()),
                T2("timeout", floate(def).mt()),
                T2("ignore_errors", bool(def).mt())
            )).mt()),
            T2("socket", assoc(def, list(
                T2("bindto", str(def, "128.211.185.166:3345").mt()),
                T2("backlog", inte(def).mt()),
                T2("ipv6_v6only", bool(def).mt()),
                T2("so_reuseport", inte(def).mt()),
                T2("so_broadcast", inte(def).mt()),
                T2("tcp_nodelay", bool(def).mt())
            )).mt()),
            T2("ftp", assoc(def, list(
                T2("overwrite", bool(def).mt()),
                T2("resume_pos", inte(def).mt()),
                T2("proxy", str(def, "tcp://squid.example.com:8000").mt())
            )).mt()),
            T2("ssl", assoc(def, list(
                T2("peer_name", bool(def).mt()),
                T2("verify_peer", bool(def).mt()),
                T2("verify_peer_name", bool(def).mt()),
                T2("allow_self_signed", bool(def).mt()),
                T2("cafile", str(def, "/path/to/cert/auth/file").mt()),
                T2("capath", str(def, "/path/to/cert/auth/dir").mt()),
                T2("local_cert", str(def, "/path/to/cert.pem").mt()),
                T2("local_pk", str(def, "/path/to/private/key.pem").mt()),
                T2("passphrase", str(def, "qwerty123").mt()),
                T2("verify_depth", inte(def).mt()),
                T2("ciphers", str(def, "ALL:!COMPLEMENTOFDEFAULT:!eNULL").mt()),
                T2("capture_peer_cert", bool(def).mt()),
                T2("capture_peer_cert_chain", bool(def).mt()),
                T2("SNI_enabled", bool(def).mt()),
                T2("disable_compression", bool(def).mt()),
                T2("peer_fingerprint", new Mt(list(str(def, "tcp://squid.example.com:8000"), arr(def))))
            )).mt()),
            T2("phar", assoc(def, list(
                T2("compress", inte(def).mt()),
                T2("metadata", mixed(def).mt())
            )).mt()),
            T2("zip", assoc(def, list(
                T2("cafile", str(def, "qwerty123").mt())
            )).mt())
        ));
    }

    // first arg
    private It<DeepType> image_type_to_mime_type(Function def)
    {
        It<String> cstNames = Cst.IMAGETYPE_.map(t -> t.a);
        return cst(ctx, cstNames);
    }

    public Iterable<DeepType> getArgType(Function builtInFunc, int argOrder)
    {
        String name = opt(builtInFunc.getName()).def("");
        if (list("stream_context_create", "stream_context_get_default", "stream_context_set_default").contains(name) && argOrder == 0) {
            return som(stream_context_create(builtInFunc));
        } else if ("stream_context_set_params".equals(builtInFunc.getName()) && argOrder == 1) {
            return som(assoc(builtInFunc, list(
                T2("notification", callable(builtInFunc).mt()),
                T2("options", mixed(builtInFunc).mt())
            )));
        } else if ("stream_context_set_option".equals(name) && argOrder == 1) {
            return som(stream_context_create(builtInFunc));
        } else if ("image_type_to_mime_type".equals(name) && argOrder == 0) {
            return image_type_to_mime_type(builtInFunc);
        } else if ("imageaffine".equals(name)) {
            if (argOrder == 1) {
                return som(assoc(builtInFunc, Tls.range(0, 6)
                    .map(n -> T2(n + "", mixed(builtInFunc).mt()))));
            } else if (argOrder == 2) {
                return som(assoc(builtInFunc, list(
                    T2("x", mixed(builtInFunc).mt()),
                    T2("y", mixed(builtInFunc).mt()),
                    T2("width", mixed(builtInFunc).mt()),
                    T2("height", mixed(builtInFunc).mt())
                )));
            }
        } else if ("imagecrop".equals(name)) {
            if (argOrder == 1) {
                return som(assoc(builtInFunc, list(
                    T2("x", mixed(builtInFunc).mt()),
                    T2("y", mixed(builtInFunc).mt()),
                    T2("width", mixed(builtInFunc).mt()),
                    T2("height", mixed(builtInFunc).mt())
                )));
            }
        } else if ("proc_open".equals(name)) {
            if (argOrder == 1) {
                return som(assocCmnt(builtInFunc, list(
                    T3("0", mixed(builtInFunc).mt(), som("STDIN")),
                    T3("1", mixed(builtInFunc).mt(), som("STDOUT")),
                    T3("2", mixed(builtInFunc).mt(), som("STDERR"))
                )));
            } else if (argOrder == 5) {
                return som(assocCmnt(builtInFunc, list(
                    T3("suppress_errors", bool(builtInFunc).mt(), non()),
                    T3("bypass_shell", bool(builtInFunc).mt(), non()),
                    T3("context", res(builtInFunc).mt(), som("= stream_context_create()")),
                    T3("binary_pipes", mixed(builtInFunc).mt(), non())
                )));
            }
        } else if ("str_pad".equals(name) && argOrder == 2) {
            return cst(ctx, list("STR_PAD_LEFT", "STR_PAD_RIGHT", "STR_PAD_BOTH"));
        } else if ("json_encode".equals(name) && argOrder == 1) {
            return cst(ctx, Cst.JSON_.map(cst -> cst.a));
        } else if ("pcntl_signal".equals(name) && argOrder == 0) {
            return cst(ctx, Cst.SIG.map(cst -> cst.a));
        } else if ("curl_setopt".equals(name) && argOrder == 1) {
            return cst(ctx, Cst.CURLOPT_().map(cst -> cst.a));
        } else if ("curl_setopt_array".equals(name) && argOrder == 1) {
            DeepType arrt = new DeepType(builtInFunc, PhpType.ARRAY, false);
            Cst.CURLOPT_().fch(t -> t.nme((cstName, getType, descr) -> {
                Mkt.cst(ctx, som(cstName)).fch(cst -> {
                    Mt valmt = getType.apply(cst.definition);
                    arrt.addKey(KeyType.mt(som(cst), cst.definition))
                        .addType(Granted(valmt), valmt.getIdeaTypes().fst().def(PhpType.UNSET))
                        .addComments(opt(descr).flt(c -> c.length() > 0));
                });
            }));
            return som(arrt);
        } else if ("file_put_contents".equals(name) && argOrder == 2) {
            return cst(ctx, list("FILE_APPEND", "FILE_USE_INCLUDE_PATH", "LOCK_EX"));
        } else if ("preg_match".equals(name) && argOrder == 3) {
            return cst(ctx, list("PREG_OFFSET_CAPTURE", "PREG_UNMATCHED_AS_NULL"));
        } else if ("preg_split".equals(name) && argOrder == 3) {
            return cst(ctx, list("PREG_SPLIT_NO_EMPTY", "PREG_SPLIT_DELIM_CAPTURE", "PREG_SPLIT_OFFSET_CAPTURE"));
        } else if ("preg_match_all".equals(name) && argOrder == 3) {
            return cst(ctx, list("PREG_SET_ORDER", "PREG_PATTERN_ORDER", "PREG_OFFSET_CAPTURE", "PREG_UNMATCHED_AS_NULL"));
        } else if ("simplexml_load_string".equals(name) && argOrder == 2
                || "simplexml_load_file".equals(name) && argOrder == 2
        ) {
            return cst(ctx, list(
                "LIBXML_NOCDATA", "LIBXML_NOBLANKS", "LIBXML_NOEMPTYTAG", "LIBXML_NOEMPTYTAG", "LIBXML_NOERROR",
                "LIBXML_NONET", "LIBXML_NOWARNING", "LIBXML_NOXMLDECL", "LIBXML_NSCLEAN", "LIBXML_PARSEHUGE",
                "LIBXML_PEDANTIC", "LIBXML_XINCLUDE", "LIBXML_ERR_ERROR", "LIBXML_ERR_FATAL", "LIBXML_ERR_NONE",
                "LIBXML_ERR_WARNING", "LIBXML_VERSION", "LIBXML_DOTTED_VERSION", "LIBXML_SCHEMA_CREATE",
                "LIBXML_BIGLINES", "LIBXML_COMPACT", "LIBXML_DTDATTR", "LIBXML_DTDLOAD", "LIBXML_DTDVALID",
                "LIBXML_HTML_NOIMPLIED", "LIBXML_HTML_NODEFDTD"
            ));
        }
        return It.non();
    }
}
