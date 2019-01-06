package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Function;
import org.klesun.deep_assoc_completion.contexts.IExprCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.resolvers.MainRes;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.It;
import org.klesun.lang.L;

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
        PhpIndex idx = PhpIndex.getInstance(def.getProject());
        L<String> cstNames = list(
            "IMAGETYPE_GIF",     // 'image/gif'
            "IMAGETYPE_JPEG",    // 'image/jpeg'
            "IMAGETYPE_PNG",     // 'image/png'
            "IMAGETYPE_SWF",     // 'application/x-shockwave-flash'
            "IMAGETYPE_PSD",     // 'image/psd'
            "IMAGETYPE_BMP",     // 'image/bmp'
            "IMAGETYPE_TIFF_II", // '(intel byte order)	image/tiff'
            "IMAGETYPE_TIFF_MM", // '(motorola byte order)	image/tiff'
            "IMAGETYPE_JPC",     // 'application/octet-stream'
            "IMAGETYPE_JP2",     // 'image/jp2'
            "IMAGETYPE_JPX",     // 'application/octet-stream'
            "IMAGETYPE_JB2",     // 'application/octet-stream'
            "IMAGETYPE_SWC",     // 'application/x-shockwave-flash'
            "IMAGETYPE_IFF",     // 'image/iff'
            "IMAGETYPE_WBMP",    // 'image/vnd.wap.wbmp'
            "IMAGETYPE_XBM",     // 'image/xbm'
            "IMAGETYPE_ICO",     // 'image/vnd.microsoft.icon'
            "IMAGETYPE_WEBP"     // 'image/webp'
        );
        return cstNames.fap(nme -> It(idx.getConstantsByName(nme)))
            .fap(cstDef -> MainRes.resolveConst(cstDef, ctx));
    }

    public It<DeepType> getArgType(Function builtInFunc, int argOrder)
    {
        if ("stream_context_create".equals(builtInFunc.getName()) && argOrder == 0) {
            return It(som(stream_context_create(builtInFunc)));
        } else if ("image_type_to_mime_type".equals(builtInFunc.getName()) && argOrder == 0) {
            return image_type_to_mime_type(builtInFunc);
        } else {
            return It.non();
        }
    }
}
