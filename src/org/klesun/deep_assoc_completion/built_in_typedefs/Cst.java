package org.klesun.deep_assoc_completion.built_in_typedefs;

import com.intellij.psi.PsiElement;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.structures.Mkt;
import org.klesun.lang.L;
import org.klesun.lang.Lang;

import static org.klesun.lang.Lang.*;

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

    public static L<T3<String, Lang.F<PsiElement, Mt>, String>> CURLOPT_()
    {
        L<T3<String, Lang.F<PsiElement, Mt>, String>> result = list();
        Lang.F<PsiElement, Mt> getType = (huj) -> Mkt.bool(huj, true).mt();
        result.add(T3("CURLOPT_AUTOREFERER"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to automatically set the Referer: field in requests where it follows a Location: redirect."));
        result.add(T3("CURLOPT_BINARYTRANSFER"          , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to return the raw output when CURLOPT_RETURNTRANSFER is used.	From PHP 5.1.3, this option has no effect: the raw output will always be returned when CURLOPT_RETURNTRANSFER is used."));
        result.add(T3("CURLOPT_COOKIESESSION"           , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to mark this as a new cookie 'session'. It will force libcurl to ignore all cookies it is about to load that are 'session cookies' from the previous session. By default, libcurl always stores and loads all cookies, independent if they are session cookies or not. Session cookies are cookies without expiry date and they are meant to be alive and existing for this 'session' only."));
        result.add(T3("CURLOPT_CERTINFO"                , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to output SSL certification information to STDERR on secure transfers.	Added in cURL 7.19.1. Available since PHP 5.3.2. Requires CURLOPT_VERBOSE to be on to have an effect."));
        result.add(T3("CURLOPT_CONNECT_ONLY"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUEtells the library to perform all the required proxy authentication and connection setup, but no data transfer. This option is implemented for HTTP, SMTP and POP3.	Added in 7.15.2. Available since PHP 5.5.0."));
        result.add(T3("CURLOPT_CRLF"                    , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to convert Unix newlines to CRLF newlines on transfers."));
        result.add(T3("CURLOPT_DNS_USE_GLOBAL_CACHE"    , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to use a global DNS cache. This option is not thread-safe and is enabled by default."));
        result.add(T3("CURLOPT_FAILONERROR"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to fail verbosely if the HTTP code returned is greater than or equal to 400. The default behavior is to return the page normally, ignoring the code."));
        result.add(T3("CURLOPT_SSL_FALSESTART"          , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to enable TLS false start.	Added in cURL 7.42.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_FILETIME"                , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to attempt to retrieve the modification date of the remote document. This value can be retrieved using the CURLINFO_FILETIME option with curl_getinfo()."));
        result.add(T3("CURLOPT_FOLLOWLOCATION"          , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to follow any 'Location: ' header that the server sends as part of the HTTP header (note this is recursive, PHP will follow as many 'Location: ' headers that it is sent, unless CURLOPT_MAXREDIRS is set)."));
        result.add(T3("CURLOPT_FORBID_REUSE"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to force the connection to explicitly close when it has finished processing, and not be pooled for reuse."));
        result.add(T3("CURLOPT_FRESH_CONNECT"           , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to force the use of a new connection instead of a cached one."));
        result.add(T3("CURLOPT_FTP_USE_EPRT"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to use EPRT (and LPRT) when doing active FTP downloads. Use FALSE to disable EPRT and LPRT and use PORT only."));
        result.add(T3("CURLOPT_FTP_USE_EPSV"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to first try an EPSV command for FTP transfers before reverting back to PASV. Set to FALSE to disable EPSV."));
        result.add(T3("CURLOPT_FTP_CREATE_MISSING_DIRS" , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to create missing directories when an FTP operation encounters a path that currently doesn't exist."));
        result.add(T3("CURLOPT_FTPAPPEND"               , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to append to the remote file instead of overwriting it."));
        result.add(T3("CURLOPT_TCP_NODELAY"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to disable TCP's Nagle algorithm, which tries to minimize the number of small packets on the network.	Available since PHP 5.2.1 for versions compiled with libcurl 7.11.2 or greater."));
        result.add(T3("CURLOPT_FTPASCII"                , (psi) -> Mkt.mixed(psi).mt(), "An alias of CURLOPT_TRANSFERTEXT. Use that instead."));
        result.add(T3("CURLOPT_FTPLISTONLY"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to only list the names of an FTP directory."));
        result.add(T3("CURLOPT_HEADER"                  , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to include the header in the output."));
        result.add(T3("CURLINFO_HEADER_OUT"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to track the handle's request string.	Available since PHP 5.1.3. The CURLINFO_ prefix is intentional."));
        result.add(T3("CURLOPT_HTTPGET"                 , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to reset the HTTP request method to GET. Since GET is the default, this is only necessary if the request method has been changed."));
        result.add(T3("CURLOPT_HTTPPROXYTUNNEL"         , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to tunnel through a given HTTP proxy."));
        result.add(T3("CURLOPT_MUTE"                    , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to be completely silent with regards to the cURL functions.	Removed in cURL 7.15.5 (You can use CURLOPT_RETURNTRANSFER instead)"));
        result.add(T3("CURLOPT_NETRC"                   , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to scan the ~/.netrc file to find a username and password for the remote site that a connection is being established with."));
        result.add(T3("CURLOPT_NOBODY"                  , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to exclude the body from the output. Request method is then set to HEAD. Changing this to FALSE does not change it to GET."));
        result.add(T3("CURLOPT_NOPROGRESS" 	            , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_NOSIGNAL"                , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to ignore any cURL function that causes a signal to be sent to the PHP process. This is turned on by default in multi-threaded SAPIs so timeout options can still be used.	Added in cURL 7.10."));
        result.add(T3("CURLOPT_PATH_AS_IS"              , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to not handle dot dot sequences.	Added in cURL 7.42.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_PIPEWAIT"                , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to wait for pipelining/multiplexing.	Added in cURL 7.43.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_POST"                    , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to do a regular HTTP POST. This POST is the normal application/x-www-form-urlencoded kind, most commonly used by HTML forms."));
        result.add(T3("CURLOPT_PUT"                     , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to HTTP PUT a file. The file to PUT must be set with CURLOPT_INFILE and CURLOPT_INFILESIZE."));
        result.add(T3("CURLOPT_RETURNTRANSFER"          , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to return the transfer as a string of the return value of curl_exec() instead of outputting it directly."));
        result.add(T3("CURLOPT_SAFE_UPLOAD"             , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to disable support for the @ prefix for uploading files in CURLOPT_POSTFIELDS, which means that values starting with @ can be safely passed as fields. CURLFile may be used for uploads instead.	Added in PHP 5.5.0 with FALSE as the default value. PHP 5.6.0 changes the default value to TRUE. PHP 7 removes this option; the CURLFile interface must be used to upload files."));
        result.add(T3("CURLOPT_SASL_IR"                 , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to enable sending the initial response in the first packet.	Added in cURL 7.31.10. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_SSL_ENABLE_ALPN"         , (psi) -> Mkt.bool(psi, false).mt(), "FALSE to disable ALPN in the SSL handshake (if the SSL backend libcurl is built to use supports it), which can be used to negotiate http2.	Added in cURL 7.36.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_SSL_ENABLE_NPN"          , (psi) -> Mkt.bool(psi, false).mt(), "FALSE to disable NPN in the SSL handshake (if the SSL backend libcurl is built to use supports it), which can be used to negotiate http2.	Added in cURL 7.36.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_SSL_VERIFYPEER"          , (psi) -> Mkt.bool(psi, false).mt(), "FALSE to stop cURL from verifying the peer's certificate. Alternate certificates to verify against can be specified with the CURLOPT_CAINFO option or a certificate directory can be specified with the CURLOPT_CAPATH option.	TRUE by default as of cURL 7.10. Default bundle installed as of cURL 7.10."));
        result.add(T3("CURLOPT_SSL_VERIFYSTATUS"        , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to verify the certificate's status.	Added in cURL 7.41.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_TCP_FASTOPEN"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to enable TCP Fast Open.	Added in cURL 7.49.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_TFTP_NO_OPTIONS"         , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to not send TFTP options requests.	Added in cURL 7.48.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_TRANSFERTEXT"            , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to use ASCII mode for FTP transfers. For LDAP, it retrieves data in plain text instead of HTML. On Windows systems, it will not set STDOUT to binary mode."));
        result.add(T3("CURLOPT_UNRESTRICTED_AUTH"       , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to keep sending the username and password when following locations (using CURLOPT_FOLLOWLOCATION), even when the hostname has changed."));
        result.add(T3("CURLOPT_UPLOAD"                  , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to prepare for an upload."));
        result.add(T3("CURLOPT_VERBOSE"                 , (psi) -> Mkt.bool(psi, true).mt(), "TRUE to output verbose information. Writes output to STDERR, or the file specified using CURLOPT_STDERR."));
        result.add(T3("CURLOPT_BUFFERSIZE"              , (psi) -> Mkt.mixed(psi).mt(), "The size of the buffer to use for each read. There is no guarantee this request will be fulfilled, however.	Added in cURL 7.10."));
        result.add(T3("CURLOPT_CLOSEPOLICY"             , (psi) -> Mkt.mixed(psi).mt(), "One of the CURLCLOSEPOLICY_* values."));
        result.add(T3("CURLOPT_CONNECTTIMEOUT"          , (psi) -> Mkt.inte(psi).mt(), "The number of seconds to wait while trying to connect. Use 0 to wait indefinitely."));
        result.add(T3("CURLOPT_CONNECTTIMEOUT_MS"       , (psi) -> Mkt.inte(psi).mt(), "The number of milliseconds to wait while trying to connect. Use 0 to wait indefinitely. If libcurl is built to use the standard system name resolver, that portion of the connect will still use full-second resolution for timeouts with a minimum timeout allowed of one second.	Added in cURL 7.16.2. Available since PHP 5.2.3."));
        result.add(T3("CURLOPT_DNS_CACHE_TIMEOUT"       , (psi) -> Mkt.inte(psi).mt(), "The number of seconds to keep DNS entries in memory. This option is set to 120 (2 minutes) by default."));
        result.add(T3("CURLOPT_EXPECT_100_TIMEOUT_MS"   , (psi) -> Mkt.mixed(psi).mt(), "The timeout for Expect: 100-continue responses in milliseconds. Defaults to 1000 milliseconds.	Added in cURL 7.36.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_FTPSSLAUTH"              , (psi) -> Mkt.mixed(psi).mt(), "The FTP authentication method (when is activated): CURLFTPAUTH_SSL (try SSL first), CURLFTPAUTH_TLS (try TLS first), or CURLFTPAUTH_DEFAULT (let cURL decide).	Added in cURL 7.12.2."));
        result.add(T3("CURLOPT_HEADEROPT"               , (psi) -> Mkt.mixed(psi).mt(), "How to deal with headers. One of the following constants: CURLHEADER_UNIFIED: the headers specified in CURLOPT_HTTPHEADER will be used in requests both to servers and proxies. With this option enabled, CURLOPT_PROXYHEADER will not have any effect. CURLHEADER_SEPARATE: makes CURLOPT_HTTPHEADER headers only get sent to a server and not to a proxy. Proxy headers must be set with CURLOPT_PROXYHEADER to get used. Note that if a non-CONNECT request is sent to a proxy, libcurl will send both server headers and proxy headers. When doing CONNECT, libcurl will send CURLOPT_PROXYHEADER headers only to the proxy and then CURLOPT_HTTPHEADER headers only to the server. Defaults to CURLHEADER_SEPARATE as of cURL 7.42.1, and CURLHEADER_UNIFIED before.	Added in cURL 7.37.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_HTTP_VERSION"            , (psi) -> Mkt.mixed(psi).mt(), "CURL_HTTP_VERSION_NONE (default, lets CURL decide which version to use), CURL_HTTP_VERSION_1_0 (forces HTTP/1.0), or CURL_HTTP_VERSION_1_1 (forces HTTP/1.1)."));
        result.add(T3("CURLOPT_HTTPAUTH" 	             , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLAUTH_ANY"                    , (psi) -> Mkt.mixed(psi).mt(), "is an alias for CURLAUTH_BASIC | CURLAUTH_DIGEST | CURLAUTH_GSSNEGOTIATE | CURLAUTH_NTLM."));
        result.add(T3("CURLAUTH_ANYSAFE"                , (psi) -> Mkt.mixed(psi).mt(), "is an alias for CURLAUTH_DIGEST | CURLAUTH_GSSNEGOTIATE | CURLAUTH_NTLM."));
        result.add(T3("CURLOPT_INFILESIZE"              , (psi) -> Mkt.mixed(psi).mt(), "The expected size, in bytes, of the file when uploading a file to a remote site. Note that using this option will not stop libcurl from sending more data, as exactly what is sent depends on CURLOPT_READFUNCTION."));
        result.add(T3("CURLOPT_LOW_SPEED_LIMIT"         , (psi) -> Mkt.mixed(psi).mt(), "The transfer speed, in bytes per second, that the transfer should be below during the count of CURLOPT_LOW_SPEED_TIME seconds before PHP considers the transfer too slow and aborts."));
        result.add(T3("CURLOPT_LOW_SPEED_TIME"          , (psi) -> Mkt.inte(psi).mt(), "The number of seconds the transfer speed should be below CURLOPT_LOW_SPEED_LIMIT before PHP considers the transfer too slow and aborts."));
        result.add(T3("CURLOPT_MAXCONNECTS"             , (psi) -> Mkt.mixed(psi).mt(), "The maximum amount of persistent connections that are allowed. When the limit is reached, CURLOPT_CLOSEPOLICY is used to determine which connection to close."));
        result.add(T3("CURLOPT_MAXREDIRS"               , (psi) -> Mkt.mixed(psi).mt(), "The maximum amount of HTTP redirections to follow. Use this option alongside CURLOPT_FOLLOWLOCATION."));
        result.add(T3("CURLOPT_PORT"                    , (psi) -> Mkt.mixed(psi).mt(), "An alternative port number to connect to."));
        result.add(T3("CURLOPT_POSTREDIR"               , (psi) -> Mkt.mixed(psi).mt(), "A bitmask of 1 (301 Moved Permanently), 2 (302 Found) and 4 (303 See Other) if the HTTP POST method should be maintained when CURLOPT_FOLLOWLOCATION is set and a specific type of redirect occurs.	Added in cURL 7.19.1. Available since PHP 5.3.2."));
        result.add(T3("CURLOPT_PROTOCOLS" 	             , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_PROXYAUTH"               , (psi) -> Mkt.mixed(psi).mt(), "The HTTP authentication method(s) to use for the proxy connection. Use the same bitmasks as described in CURLOPT_HTTPAUTH. For proxy authentication, only CURLAUTH_BASIC and CURLAUTH_NTLM are currently supported.	Added in cURL 7.10.7."));
        result.add(T3("CURLOPT_PROXYPORT"               , (psi) -> Mkt.mixed(psi).mt(), "The port number of the proxy to connect to. This port number can also be set in CURLOPT_PROXY."));
        result.add(T3("CURLOPT_PROXYTYPE"               , (psi) -> Mkt.mixed(psi).mt(), "Either CURLPROXY_HTTP (default), CURLPROXY_SOCKS4, CURLPROXY_SOCKS5, CURLPROXY_SOCKS4A or CURLPROXY_SOCKS5_HOSTNAME.	Added in cURL 7.10."));
        result.add(T3("CURLOPT_REDIR_PROTOCOLS"         , (psi) -> Mkt.mixed(psi).mt(), "Bitmask of CURLPROTO_* values. If used, this bitmask limits what protocols libcurl may use in a transfer that it follows to in a redirect when CURLOPT_FOLLOWLOCATION is enabled. This allows you to limit specific transfers to only be allowed to use a subset of protocols in redirections. By default libcurl will allow all protocols except for FILE and SCP. This is a difference compared to pre-7.19.4 versions which unconditionally would follow to all protocols supported. See also CURLOPT_PROTOCOLS for protocol constant values.	Added in cURL 7.19.4."));
        result.add(T3("CURLOPT_RESUME_FROM"             , (psi) -> Mkt.mixed(psi).mt(), "The offset, in bytes, to resume a transfer from."));
        result.add(T3("CURLOPT_SSL_OPTIONS"             , (psi) -> Mkt.mixed(psi).mt(), "Set SSL behavior options, which is a bitmask of any of the following constants: CURLSSLOPT_ALLOW_BEAST: do not attempt to use any workarounds for a security flaw in the SSL3 and TLS1.0 protocols. CURLSSLOPT_NO_REVOKE: disable certificate revocation checks for those SSL backends where such behavior is present.	Added in cURL 7.25.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_SSL_VERIFYHOST"          , (psi) -> Mkt.mixed(psi).mt(), "1 to check the existence of a common name in the SSL peer certificate. 2 to check the existence of a common name and also verify that it matches the hostname provided. 0 to not check the names. In production environments the value of this option should be kept at 2 (default value).	Support for value 1 removed in cURL 7.28.1."));
        result.add(T3("CURLOPT_SSLVERSION"              , (psi) -> Mkt.mixed(psi).mt(), "One of CURL_SSLVERSION_DEFAULT (0), CURL_SSLVERSION_TLSv1 (1), CURL_SSLVERSION_SSLv2 (2), CURL_SSLVERSION_SSLv3 (3), CURL_SSLVERSION_TLSv1_0 (4), CURL_SSLVERSION_TLSv1_1 (5) or CURL_SSLVERSION_TLSv1_2 (6)."));
        result.add(T3("CURLOPT_STREAM_WEIGHT"           , (psi) -> Mkt.mixed(psi).mt(), "Set the numerical stream weight (a number between 1 and 256).	Added in cURL 7.46.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_TIMECONDITION"           , (psi) -> Mkt.mixed(psi).mt(), "How CURLOPT_TIMEVALUE is treated. Use CURL_TIMECOND_IFMODSINCE to return the page only if it has been modified since the time specified in CURLOPT_TIMEVALUE. If it hasn't been modified, a '304 Not Modified' header will be returned assuming CURLOPT_HEADER is TRUE. Use CURL_TIMECOND_IFUNMODSINCE for the reverse effect. CURL_TIMECOND_IFMODSINCE is the default."));
        result.add(T3("CURLOPT_TIMEOUT"                 , (psi) -> Mkt.inte(psi).mt(), "The maximum number of seconds to allow cURL functions to execute."));
        result.add(T3("CURLOPT_TIMEOUT_MS"              , (psi) -> Mkt.inte(psi).mt(), "The maximum number of milliseconds to allow cURL functions to execute. If libcurl is built to use the standard system name resolver, that portion of the connect will still use full-second resolution for timeouts with a minimum timeout allowed of one second.	Added in cURL 7.16.2. Available since PHP 5.2.3."));
        result.add(T3("CURLOPT_TIMEVALUE"               , (psi) -> Mkt.mixed(psi).mt(), "The time in seconds since January 1st, 1970. The time will be used by CURLOPT_TIMECONDITION. By default, CURL_TIMECOND_IFMODSINCE is used."));
        result.add(T3("CURLOPT_MAX_RECV_SPEED_LARGE"    , (psi) -> Mkt.mixed(psi).mt(), "If a download exceeds this speed (counted in bytes per second) on cumulative average during the transfer, the transfer will pause to keep the average rate less than or equal to the parameter value. Defaults to unlimited speed.	Added in cURL 7.15.5. Available since PHP 5.4.0."));
        result.add(T3("CURLOPT_MAX_SEND_SPEED_LARGE"    , (psi) -> Mkt.mixed(psi).mt(), "If an upload exceeds this speed (counted in bytes per second) on cumulative average during the transfer, the transfer will pause to keep the average rate less than or equal to the parameter value. Defaults to unlimited speed.	Added in cURL 7.15.5. Available since PHP 5.4.0."));
        result.add(T3("CURLOPT_SSH_AUTH_TYPES"          , (psi) -> Mkt.mixed(psi).mt(), "A bitmask consisting of one or more of CURLSSH_AUTH_PUBLICKEY, CURLSSH_AUTH_PASSWORD, CURLSSH_AUTH_HOST, CURLSSH_AUTH_KEYBOARD. Set to CURLSSH_AUTH_ANY to let libcurl pick one.	Added in cURL 7.16.1."));
        result.add(T3("CURLOPT_IPRESOLVE"               , (psi) -> Mkt.mixed(psi).mt(), "Allows an application to select what kind of IP addresses to use when resolving host names. This is only interesting when using host names that resolve addresses using more than one version of IP, possible values are CURL_IPRESOLVE_WHATEVER, CURL_IPRESOLVE_V4, CURL_IPRESOLVE_V6, by default CURL_IPRESOLVE_WHATEVER.	Added in cURL 7.10.8."));
        result.add(T3("CURLOPT_FTP_FILEMETHOD"          , (psi) -> Mkt.mixed(psi).mt(), "Tell curl which method to use to reach a file on a FTP(S) server. Possible values are CURLFTPMETHOD_MULTICWD, CURLFTPMETHOD_NOCWD and CURLFTPMETHOD_SINGLECWD.	Added in cURL 7.15.1. Available since PHP 5.3.0."));
        result.add(T3("CURLOPT_CAINFO"                  , (psi) -> Mkt.mixed(psi).mt(), "The name of a file holding one or more certificates to verify the peer with. This only makes sense when used in combination with CURLOPT_SSL_VERIFYPEER.	Might require an absolute path."));
        result.add(T3("CURLOPT_CAPATH"                  , (psi) -> Mkt.mixed(psi).mt(), "A directory that holds multiple CA certificates. Use this option alongside CURLOPT_SSL_VERIFYPEER."));
        result.add(T3("CURLOPT_COOKIE"                  , (psi) -> Mkt.mixed(psi).mt(), "The contents of the 'Cookie: ' header to be used in the HTTP request. Note that multiple cookies are separated with a semicolon followed by a space (e.g., 'fruit=apple; colour=red')"));
        result.add(T3("CURLOPT_COOKIEFILE"              , (psi) -> Mkt.mixed(psi).mt(), "The name of the file containing the cookie data. The cookie file can be in Netscape format, or just plain HTTP-style headers dumped into a file. If the name is an empty string, no cookies are loaded, but cookie handling is still enabled."));
        result.add(T3("CURLOPT_COOKIEJAR"               , (psi) -> Mkt.mixed(psi).mt(), "The name of a file to save all internal cookies to when the handle is closed, e.g. after a call to curl_close."));
        result.add(T3("CURLOPT_CUSTOMREQUEST" 	         , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_DEFAULT_PROTOCOL" 	     , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_DNS_INTERFACE" 	         , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_DNS_LOCAL_IP4" 	         , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_DNS_LOCAL_IP6" 	         , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_EGDSOCKET"               , (psi) -> Mkt.mixed(psi).mt(), "Like CURLOPT_RANDOM_FILE, except a filename to an Entropy Gathering Daemon socket."));
        result.add(T3("CURLOPT_ENCODING"                , (psi) -> Mkt.mixed(psi).mt(), "The contents of the 'Accept-Encoding: ' header. This enables decoding of the response. Supported encodings are 'identity', 'deflate', and 'gzip'. If an empty string, '', is set, a header containing all supported encoding types is sent.	Added in cURL 7.10."));
        result.add(T3("CURLOPT_FTPPORT"                 , (psi) -> Mkt.mixed(psi).mt(), "The value which will be used to get the IP address to use for the FTP 'PORT' instruction. The 'PORT' instruction tells the remote server to connect to our specified IP address. The string may be a plain IP address, a hostname, a network interface name (under Unix), or just a plain '-' to use the systems default IP address."));
        result.add(T3("CURLOPT_INTERFACE"               , (psi) -> Mkt.mixed(psi).mt(), "The name of the outgoing network interface to use. This can be an interface name, an IP address or a host name."));
        result.add(T3("CURLOPT_KEYPASSWD"               , (psi) -> Mkt.mixed(psi).mt(), "The password required to use the CURLOPT_SSLKEY or CURLOPT_SSH_PRIVATE_KEYFILE private key.	Added in cURL 7.16.1."));
        result.add(T3("CURLOPT_KRB4LEVEL"               , (psi) -> Mkt.mixed(psi).mt(), "The KRB4 (Kerberos 4) security level. Any of the following values (in order from least to most powerful) are valid: 'clear', 'safe', 'confidential', 'private'.. If the string does not match one of these, 'private' is used. Setting this option to NULL will disable KRB4 security. Currently KRB4 security only works with FTP transactions."));
        result.add(T3("CURLOPT_LOGIN_OPTIONS"           , (psi) -> Mkt.mixed(psi).mt(), "Can be used to set protocol specific login options, such as the preferred authentication mechanism via 'AUTH=NTLM' or 'AUTH=*', and should be used in conjunction with the CURLOPT_USERNAME option.	Added in cURL 7.34.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_PINNEDPUBLICKEY"         , (psi) -> Mkt.mixed(psi).mt(), "Set the pinned public key. The string can be the file name of your pinned public key. The file format expected is 'PEM' or 'DER'. The string can also be any number of base64 encoded sha256 hashes preceded by 'sha256//' and separated by ';'.	Added in cURL 7.39.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_POSTFIELDS"              , (psi) -> Mkt.mixed(psi).mt(), "The full data to post in a HTTP 'POST' operation. To post a file, prepend a filename with @ and use the full path. The filetype can be explicitly specified by following the filename with the type in the format ';type=mimetype'. This parameter can either be passed as a urlencoded string like 'para1=val1&para2=val2&...' or as an array with the field name as key and field data as value. If value is an array, the Content-Type header will be set to multipart/form-data. As of PHP 5.2.0, value must be an array if files are passed to this option with the @ prefix. As of PHP 5.5.0, the @ prefix is deprecated and files can be sent using CURLFile. The @ prefix can be disabled for safe passing of values beginning with @ by setting the CURLOPT_SAFE_UPLOAD option to TRUE."));
        result.add(T3("CURLOPT_PRIVATE"                 , (psi) -> Mkt.mixed(psi).mt(), "Any data that should be associated with this cURL handle. This data can subsequently be retrieved with the CURLINFO_PRIVATE option of curl_getinfo(). cURL does nothing with this data. When using a cURL multi handle, this private data is typically a unique key to identify a standard cURL handle.	Added in cURL 7.10.3."));
        result.add(T3("CURLOPT_PROXY"                   , (psi) -> Mkt.mixed(psi).mt(), "The HTTP proxy to tunnel requests through."));
        result.add(T3("CURLOPT_PROXY_SERVICE_NAME"      , (psi) -> Mkt.mixed(psi).mt(), "The proxy authentication service name.	Added in cURL 7.34.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_PROXYUSERPWD"            , (psi) -> Mkt.mixed(psi).mt(), "A username and password formatted as '[username]:[password]' to use for the connection to the proxy."));
        result.add(T3("CURLOPT_RANDOM_FILE"             , (psi) -> Mkt.mixed(psi).mt(), "A filename to be used to seed the random number generator for SSL."));
        result.add(T3("CURLOPT_RANGE"                   , (psi) -> Mkt.mixed(psi).mt(), "Range(s) of data to retrieve in the format 'X-Y' where X or Y are optional. HTTP transfers also support several intervals, separated with commas in the format 'X-Y,N-M'."));
        result.add(T3("CURLOPT_REFERER"                 , (psi) -> Mkt.mixed(psi).mt(), "The contents of the 'Referer: ' header to be used in a HTTP request."));
        result.add(T3("CURLOPT_SERVICE_NAME"            , (psi) -> Mkt.mixed(psi).mt(), "The authentication service name.	Added in cURL 7.43.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_SSH_HOST_PUBLIC_KEY_MD5" , (psi) -> Mkt.mixed(psi).mt(), "A string containing 32 hexadecimal digits. The string should be the MD5 checksum of the remote host's public key, and libcurl will reject the connection to the host unless the md5sums match. This option is only for SCP and SFTP transfers.	Added in cURL 7.17.1."));
        result.add(T3("CURLOPT_SSH_PUBLIC_KEYFILE"      , (psi) -> Mkt.mixed(psi).mt(), "The file name for your public key. If not used, libcurl defaults to $HOME/.ssh/id_dsa.pub if the HOME environment variable is set, and just 'id_dsa.pub' in the current directory if HOME is not set.	Added in cURL 7.16.1."));
        result.add(T3("CURLOPT_SSH_PRIVATE_KEYFILE"     , (psi) -> Mkt.mixed(psi).mt(), "The file name for your private key. If not used, libcurl defaults to $HOME/.ssh/id_dsa if the HOME environment variable is set, and just 'id_dsa' in the current directory if HOME is not set. If the file is password-protected, set the password with CURLOPT_KEYPASSWD.	Added in cURL 7.16.1."));
        result.add(T3("CURLOPT_SSL_CIPHER_LIST"         , (psi) -> Mkt.mixed(psi).mt(), "A list of ciphers to use for SSL. For example, RC4-SHA and TLSv1 are valid cipher lists."));
        result.add(T3("CURLOPT_SSLCERT"                 , (psi) -> Mkt.mixed(psi).mt(), "The name of a file containing a PEM formatted certificate."));
        result.add(T3("CURLOPT_SSLCERTPASSWD"           , (psi) -> Mkt.mixed(psi).mt(), "The password required to use the CURLOPT_SSLCERT certificate."));
        result.add(T3("CURLOPT_SSLCERTTYPE"             , (psi) -> Mkt.mixed(psi).mt(), "The format of the certificate. Supported formats are 'PEM' (default), 'DER', and 'ENG'.	Added in cURL 7.9.3."));
        result.add(T3("CURLOPT_SSLENGINE"               , (psi) -> Mkt.mixed(psi).mt(), "The identifier for the crypto engine of the private SSL key specified in CURLOPT_SSLKEY."));
        result.add(T3("CURLOPT_SSLENGINE_DEFAULT"       , (psi) -> Mkt.mixed(psi).mt(), "The identifier for the crypto engine used for asymmetric crypto operations."));
        result.add(T3("CURLOPT_SSLKEY"                  , (psi) -> Mkt.mixed(psi).mt(), "The name of a file containing a private SSL key."));
        result.add(T3("CURLOPT_SSLKEYPASSWD" 	         , (psi) -> Mkt.mixed(psi).mt(), ""));
        result.add(T3("CURLOPT_SSLKEYTYPE"              , (psi) -> Mkt.mixed(psi).mt(), "The key type of the private SSL key specified in CURLOPT_SSLKEY. Supported key types are 'PEM' (default), 'DER', and 'ENG'."));
        result.add(T3("CURLOPT_UNIX_SOCKET_PATH"        , (psi) -> Mkt.mixed(psi).mt(), "Enables the use of Unix domain sockets as connection endpoint and sets the path to the given string.	Added in cURL 7.40.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_URL"                     , (psi) -> Mkt.mixed(psi).mt(), "The URL to fetch. This can also be set when initializing a session with curl_init()."));
        result.add(T3("CURLOPT_USERAGENT"               , (psi) -> Mkt.mixed(psi).mt(), "The contents of the 'User-Agent: ' header to be used in a HTTP request."));
        result.add(T3("CURLOPT_USERNAME"                , (psi) -> Mkt.mixed(psi).mt(), "The user name to use in authentication.	Added in cURL 7.19.1. Available since PHP 5.5.0."));
        result.add(T3("CURLOPT_USERPWD"                 , (psi) -> Mkt.mixed(psi).mt(), "A username and password formatted as '[username]:[password]' to use for the connection."));
        result.add(T3("CURLOPT_XOAUTH2_BEARER"          , (psi) -> Mkt.mixed(psi).mt(), "Specifies the OAuth 2.0 access token.	Added in cURL 7.33.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_CONNECT_TO"              , (psi) -> Mkt.mixed(psi).mt(), "Connect to a specific host and port instead of the URL's host and port. Accepts an array of strings with the format HOST:PORT:CONNECT-TO-HOST:CONNECT-TO-PORT.	Added in cURL 7.49.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_HTTP200ALIASES"          , (psi) -> Mkt.mixed(psi).mt(), "An array of HTTP 200 responses that will be treated as valid responses and not as errors.	Added in cURL 7.10.3."));
        result.add(T3("CURLOPT_HTTPHEADER"              , (psi) -> Mkt.mixed(psi).mt(), "An array of HTTP header fields to set, in the format array('Content-type: text/plain', 'Content-length: 100')"));
        result.add(T3("CURLOPT_POSTQUOTE"               , (psi) -> Mkt.mixed(psi).mt(), "An array of FTP commands to execute on the server after the FTP request has been performed."));
        result.add(T3("CURLOPT_PROXYHEADER"             , (psi) -> Mkt.mixed(psi).mt(), "An array of custom HTTP headers to pass to proxies.	Added in cURL 7.37.0. Available since PHP 7.0.7."));
        result.add(T3("CURLOPT_QUOTE"                   , (psi) -> Mkt.mixed(psi).mt(), "An array of FTP commands to execute on the server prior to the FTP request."));
        result.add(T3("CURLOPT_RESOLVE"                 , (psi) -> Mkt.mixed(psi).mt(), "Provide a custom address for a specific host and port pair. An array of hostname, port, and IP address strings, each element separated by a colon. In the format: array('example.com:80:127.0.0.1')	Added in cURL 7.21.3. Available since PHP 5.5.0."));
        result.add(T3("CURLOPT_FILE"                    , (psi) -> Mkt.mixed(psi).mt(), "The file that the transfer should be written to. The default is STDOUT (the browser window)."));
        result.add(T3("CURLOPT_INFILE"                  , (psi) -> Mkt.mixed(psi).mt(), "The file that the transfer should be read from when uploading."));
        result.add(T3("CURLOPT_STDERR"                  , (psi) -> Mkt.mixed(psi).mt(), "An alternative location to output errors to instead of STDERR."));
        result.add(T3("CURLOPT_WRITEHEADER"             , (psi) -> Mkt.mixed(psi).mt(), "he file that the header part of the transfer is written to."));
        return result;
    }
}
