<?php
namespace Gtl\AmadeusSoapActions;
use Lib\Config;
use Lib\Utils\Random;
use Gtl\AmadeusSoapActions\AmadeusSoapHeadersMaker;
use Gtl\AmadeusSession;
/**
 *
 * How to use.
 *
 * TODO: usage example
 *
 * After request is made, you can access metadata if needed:
 *
 *     $this->requestMetadata
 *     $this->responseMetadata
 *
 * or raw data:
 *
 *     $client = $this->soapClient;
 *     print $client->__getLastRequestHeaders().PHP_EOL;
 *     print $client->__getLastRequest().PHP_EOL.PHP_EOL;
 *     print $client->__getLastResponseHeaders().PHP_EOL;
 *     print $client->__getLastResponse().PHP_EOL;
 *
 */
abstract class AbstractAmadeusSoapAction
{
    // Must not be null, expected to be re-defined in the child class
    const SOAP_FUNCTION = null;
    const SOAP_ACTION = null;

    /**
     * @throws SoapException
     */
    public function performRequest(string $function, string $action, array $params, string $pcc = null)
    {
        $this->soapClient = new \SoapClient('/path/to/wsdl', [
            'location' => 'https://endpoint.com?WSDL',
            'trace' => 1,
            // SOAP_COMPRESSION_ACCEPT | SOAP_COMPRESSION_GZIP -- to accept responses in gzip & deflate
            // 9 -- compression level, to send requests in gzip
            'compression' => SOAP_COMPRESSION_ACCEPT | SOAP_COMPRESSION_GZIP | 9,
            'exceptions' => 1,
        ]);
        $response = $this->soapClient->{$function}($params);
        return $response;
    }
    // Expected to be overriden by child class, used to perform data format transformations
    abstract protected static function transformRequestParams(array $params);
    abstract protected static function transformResponse(\stdClass $data);
    public function execute(array $params = [], string $pcc = null)
    {
        $params = static::transformRequestParams($params);
        $res = $this->performRequest(static::SOAP_FUNCTION, static::SOAP_ACTION, $params, $pcc);
        return static::transformResponse($res);
    }
}