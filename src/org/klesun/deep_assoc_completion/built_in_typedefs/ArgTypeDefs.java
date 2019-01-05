package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.jetbrains.php.lang.psi.elements.Function;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.lang.Lang;

import static org.klesun.deep_assoc_completion.structures.Mkt.*;
import static org.klesun.lang.Lang.T2;

public class ArgTypeDefs {

    // first arg
    public static DeepType stream_context_create(Function def)
    {
        return assoc(def, Lang.list(
            T2("http", assoc(def, Lang.list(
                T2("header", str(def, "Content-type: application/x-www-form-urlencoded\\r\\n").mt()),
                T2("method", new Mt(Lang.list("GET", "POST", "OPTIONS", "PUT", "HEAD", "DELETE", "CONNECT", "TRACE", "PATCH").map(m -> str(def, m)))),
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
            T2("socket", assoc(def, Lang.list(
                T2("bindto", str(def, "128.211.185.166:3345").mt()),
                T2("backlog", inte(def).mt()),
                T2("ipv6_v6only", bool(def).mt()),
                T2("so_reuseport", inte(def).mt()),
                T2("so_broadcast", inte(def).mt()),
                T2("tcp_nodelay", bool(def).mt())
            )).mt()),
            T2("ftp", assoc(def, Lang.list(
                T2("overwrite", bool(def).mt()),
                T2("resume_pos", inte(def).mt()),
                T2("proxy", str(def, "tcp://squid.example.com:8000").mt())
            )).mt()),
            T2("ssl", assoc(def, Lang.list(
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
                T2("peer_fingerprint", new Mt(Lang.list(str(def, "tcp://squid.example.com:8000"), arr(def))))
            )).mt()),
            T2("phar", assoc(def, Lang.list(
                T2("compress", inte(def).mt()),
                T2("metadata", mixed(def).mt())
            )).mt()),
            T2("zip", assoc(def, Lang.list(
                T2("cafile", str(def, "qwerty123").mt())
            )).mt())
        ));
    }
}
