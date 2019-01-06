package org.klesun.deep_assoc_completion.built_in_typedefs;

import org.klesun.lang.L;
import org.klesun.lang.Lang;

import static org.klesun.lang.Lang.T2;
import static org.klesun.lang.Lang.list;

/** PHP constants */
public class Cst {
    /** T2(constantName, mimeTypeStr) */
    final public static L<Lang.T2<String, String>> IMAGETYPE_ = list(
        T2("IMAGETYPE_GIF"     , "image/gif"),
        T2("IMAGETYPE_JPEG"    , "image/jpeg"),
        T2("IMAGETYPE_PNG"     , "image/png"),
        T2("IMAGETYPE_SWF"     , "application/x-shockwave-flash"),
        T2("IMAGETYPE_PSD"     , "image/psd"),
        T2("IMAGETYPE_BMP"     , "image/bmp"),
        T2("IMAGETYPE_TIFF_II" , "(intel byte order)	image/tiff"),
        T2("IMAGETYPE_TIFF_MM" , "(motorola byte order)	image/tiff"),
        T2("IMAGETYPE_JPC"     , "application/octet-stream"),
        T2("IMAGETYPE_JP2"     , "image/jp2"),
        T2("IMAGETYPE_JPX"     , "application/octet-stream"),
        T2("IMAGETYPE_JB2"     , "application/octet-stream"),
        T2("IMAGETYPE_SWC"     , "application/x-shockwave-flash"),
        T2("IMAGETYPE_IFF"     , "image/iff"),
        T2("IMAGETYPE_WBMP"    , "image/vnd.wap.wbmp"),
        T2("IMAGETYPE_XBM"     , "image/xbm"),
        T2("IMAGETYPE_ICO"     , "image/vnd.microsoft.icon"),
        T2("IMAGETYPE_WEBP"    , "image/webp")
    );
    final public static L<T2<String, Integer>> JSON_ = list(
        T2("JSON_HEX_TAG", 1),
        T2("JSON_HEX_AMP", 2),
        T2("JSON_HEX_APOS", 4),
        T2("JSON_HEX_QUOT", 8),
        T2("JSON_FORCE_OBJECT", 16),
        T2("JSON_NUMERIC_CHECK", 32),
        T2("JSON_UNESCAPED_SLASHES", 64),
        T2("JSON_PRETTY_PRINT", 128),
        T2("JSON_UNESCAPED_UNICODE", 256),
        T2("JSON_PARTIAL_OUTPUT_ON_ERROR", 512),
        T2("JSON_PRESERVE_ZERO_FRACTION", 1024),
        T2("JSON_UNESCAPED_LINE_TERMINATORS", 2048),
        T2("JSON_OBJECT_AS_ARRAY", 1),
        T2("JSON_BIGINT_AS_STRING", 2),
        T2("JSON_INVALID_UTF8_IGNORE", 1048576),
        T2("JSON_INVALID_UTF8_SUBSTITUTE", 2097152),
        T2("JSON_ERROR_NONE", 0),
        T2("JSON_ERROR_DEPTH", 1),
        T2("JSON_ERROR_STATE_MISMATCH", 2),
        T2("JSON_ERROR_CTRL_CHAR", 3),
        T2("JSON_ERROR_SYNTAX", 4),
        T2("JSON_ERROR_UTF8", 5),
        T2("JSON_ERROR_RECURSION", 6),
        T2("JSON_ERROR_INF_OR_NAN", 7),
        T2("JSON_ERROR_UNSUPPORTED_TYPE", 8),
        T2("JSON_ERROR_INVALID_PROPERTY_NAME", 9),
        T2("JSON_ERROR_UTF16", 0)
    );
    final public static L<T2<String, Integer>> SIG = list(
        T2("SIGHUP", 1),
        T2("SIGINT", 2),
        T2("SIGQUIT", 3),
        T2("SIGILL", 4),
        T2("SIGTRAP", 5),
        T2("SIGABRT", 6),
        T2("SIGIOT", 6),
        T2("SIGBUS", 7),
        T2("SIGFPE", 8),
        T2("SIGKILL", 9),
        T2("SIGUSR1", 10),
        T2("SIGSEGV", 11),
        T2("SIGUSR2", 12),
        T2("SIGPIPE", 13),
        T2("SIGALRM", 14),
        T2("SIGTERM", 15),
        T2("SIGSTKFLT", 16),
        T2("SIGCLD", 17),
        T2("SIGCHLD", 17),
        T2("SIGCONT", 18),
        T2("SIGSTOP", 19),
        T2("SIGTSTP", 20),
        T2("SIGTTIN", 21),
        T2("SIGTTOU", 22),
        T2("SIGURG", 23),
        T2("SIGXCPU", 24),
        T2("SIGXFSZ", 25),
        T2("SIGVTALRM", 26),
        T2("SIGPROF", 27),
        T2("SIGWINCH", 28),
        T2("SIGPOLL", 29),
        T2("SIGIO", 29),
        T2("SIGPWR", 30),
        T2("SIGSYS", 31)
    );
}
