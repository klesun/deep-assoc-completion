<?php
namespace DeepTest;

use App\Models\City;
use Lib\ParamValidation\DictP;
use Lib\ParamValidation\ListP;
use Lib\ParamValidation\StringP;
use Lib\ResultGen;
use Lib\Utils\Fp;
use NeptuniaNs\Ksha;
use SomeCls123;
use TouhouNs\MarisaKirisame;
use TouhouNs\ReimuHakurei;

interface IExactKeysUnitTest
{
    /** @param $params = ['age' => 18, 'price' => '240.00'] */
    function provideInheritDocInterface($params);
}

abstract class AbstractExactKeysUnitTest
{
    public function createSale()
    {
        return ['bytes' => 123, 'timeoutMs' => 30 * 1000];
    }

    /**
     * @param $sale = self::createSale()
     * @param $pax = ['age' => 18, 'price' => '240.00']
     */
    abstract function provideInheritDocAbstract($sale, $pax);
}

/**
 * unlike UnitTest.php, this test not just checks that actual result  has _at least_
 * such keys, but it tests that it has _exactly_ such keys, without extras
 */
class ExactKeysUnitTest extends AbstractExactKeysUnitTest implements IExactKeysUnitTest
{
    public function provideInheritDocInterface($params)
    {
        $params[''];
        return [[$params, ['age', 'price']]];
    }

    public function provideInheritDocAbstract($sale, $pax)
    {
        $sale[''];
        $pax[''];
        return [
            [$sale, ['bytes', 'timeoutMs']],
            [$pax, ['age', 'price']],
        ];
    }

    public function provideAssocBuiltIns($handle)
    {
        $handle = curl_init('google.com');
        $response = curl_exec($handle);
        $curlInfo = curl_getinfo($handle);
        $curlInfo[''];
        $list[] = [$curlInfo, [
            'url', // 'http://google.com/',
            'content_type', // 'text/html; charset=UTF-8',
            'http_code', // 301,
            'header_size', // 321,
            'request_size', // 49,
            'filetime', // -1,
            'ssl_verify_result', // 0,
            'redirect_count', // 0,
            'total_time', // 0.060094000000000002,
            'namelookup_time', // 0.028378,
            'connect_time', // 0.038482000000000002,
            'pretransfer_time', // 0.038517999999999997,
            'size_upload', // 0.0,
            'size_download', // 219.0,
            'speed_download', // 3650.0,
            'speed_upload', // 0.0,
            'download_content_length', // 219.0,
            'upload_content_length', // -1.0,
            'starttransfer_time', // 0.060032000000000002,
            'redirect_time', // 0.0,
            'redirect_url', // 'http://www.google.com/',
            'primary_ip', // '172.217.21.142',
            'certinfo', // [],
            'primary_port', // 80,
            'local_ip', // '10.128.8.117',
            'local_port', // 57382,
        ]];
        $streamMeta = stream_get_meta_data(STDIN);
        $streamMeta[''];
        $list[] = [$streamMeta, [
            'timed_out', // false,
            'blocked', // true,
            'eof', // false,
            'wrapper_type', // 'PHP',
            'stream_type', // 'STDIO',
            'mode', // 'r',
            'unread_bytes', // 0,
            'seekable', // true,
            'uri', // 'php://stdin',
        ]];
        $sqlLinks = mysqli_get_links_stats();
        $list[] = [$sqlLinks, [
            'total',
            'active_plinks',
            'cached_plinks',
        ]];
        $localeMeta = localeconv();
        $localeMeta[''];
        $list[] = [$localeMeta, [
            'decimal_point', // '.',
            'thousands_sep', // '',
            'int_curr_symbol', // '',
            'currency_symbol', // '',
            'mon_decimal_point', // '',
            'mon_thousands_sep', // '',
            'positive_sign', // '',
            'negative_sign', // '',
            'int_frac_digits', // 127,
            'frac_digits', // 127,
            'p_cs_precedes', // 127,
            'p_sep_by_space', // 127,
            'n_cs_precedes', // 127,
            'n_sep_by_space', // 127,
            'p_sign_posn', // 127,
            'n_sign_posn', // 127,
            'grouping', // [],
            'mon_grouping', // [],
        ]];
        $procMeta = proc_get_status($handle);
        $procMeta[''];
        $list[] = [$procMeta, [
            'command', // 'ls', string - The command string that was passed to proc_open().
            'pid', // 29879, int - process id
            'running', // false, bool - TRUE if the process is still running, FALSE if it has terminated.
            'signaled', // false, bool - TRUE if the child process has been terminated by an uncaught signal. Always set to FALSE on Windows.
            'stopped', // false, bool - TRUE if the child process has been stopped by a signal. Always set to FALSE on Windows.
            'exitcode', // 0, int - The exit code returned by the process (which is only meaningful if running is FALSE). Only first call of this function return real value, next calls return -1.
            'termsig', // 0, int - The number of the signal that caused the child process to terminate its execution (only meaningful if signaled is TRUE).
            'stopsig', // 0, int - The number of the signal that caused the child process to stop its execution (only meaningful if stopped is TRUE).
        ]];
        $rusage = getrusage();
        $rusage[''];
        $list[] = [$rusage, [
            'ru_oublock', // 528,
            'ru_inblock', // 0,
            'ru_msgsnd', // 0,
            'ru_msgrcv', // 0,
            'ru_maxrss', // 24176,
            'ru_ixrss', // 0,
            'ru_idrss', // 0,
            'ru_minflt', // 1650,
            'ru_majflt', // 0,
            'ru_nsignals', // 0,
            'ru_nvcsw', // 224,
            'ru_nivcsw', // 0,
            'ru_nswap', // 0,
            'ru_utime.tv_usec', // 52714,
            'ru_utime.tv_sec', // 0,
            'ru_stime.tv_usec', // 11714,
            'ru_stime.tv_sec', // 0,
        ]];
        $lastError = error_get_last();
        $lastError[''];
        $lastError['type'] === E_CORE_WARNING;
        $list[] = [$lastError, [
            'type', // 2,
            'message', // 'proc_get_status() expects parameter 1 to be resource, null given',
            'file', // 'php shell code',
            'line', // 1,
        ]];
        $dnsRecord = dns_get_record('google.com');
        $dnsRecord[0][''];
        $list[] = [$dnsRecord[rand()], [
            'host', // 'google.com',
            'class', // 'IN',
            'ttl', // 60,
            'type', // 'SOA',
            'mname', // 'ns1.google.com',
            'rname', // 'dns-admin.google.com',
            'serial', // 210413966,
            'refresh', // 900,
            'retry', // 900,
            'expire', // 1800,
            'minimum-ttl',
            'flags', // 0,
            'tag', // 'issue',
            'value', // 'pki.goog▒',
        ]];
        $stat = stat('/home/klesun'); // same for fstat(), lstat()
        $stat[''];
        $list[] = [$stat, [
            'dev', // 2049,
            'ino', // 13238274,
            'mode', // 16877,ob_get_status
            'nlink', // 30,
            'uid', // 1000,
            'gid', // 1000,
            'rdev', // 0,
            'size', // 4096,
            'atime', // 1535395568,
            'mtime', // 1535390333,
            'ctime', // 1535390333,
            'blksize', // 4096,
            'blocks', // 8,
        ]];
        $stat = ob_get_status(); // array of assoc arrays if true passed
        $stat[''];
        $list[] = [$stat, [
            'name', // 'default output handler',
            'type', // 0,
            'flags', // 20592,
            'level', // 0,
            'chunk_size', // 0,
            'buffer_size', // 16384,
            'buffer_used', // 1,
        ]];
        $imgInfo = getimagesize('avatar.png');
        $imgInfo[''];
        $list[] = [$imgInfo, [
            '0', // 200,
            '1', // 200,
            '2', // 3,
            '3', // 'width="200" height="200"',
            'bits', // 8,
            'mime', // 'image/png',
            'channels',
        ]];
        $urlInfo = parse_url('https://mail.google.com/mail/u/1/&onpage=40#inbox/FMfcgxvzKQhBBjPqwdDkmmrgBMGfHvjz?page=5');
        $urlInfo[''];
        $list[] = [$urlInfo, [
            'scheme', // 'https',
            'host', // 'mail.google.com',
            'port', // 80,
            'path', // '/mail/u/1/&onpage=40',
            'fragment', // 'inbox/FMfcgxvzKQhBBjPqwdDkmmrgBMGfHvjz?page=5',
            'query', //
            'user', //
            'pass', //
        ]];
        image_type_to_mime_type() === 'image/png';
        return $list;
    }

    public function provide_debug_backtrace()
    {
        $nodes = debug_backtrace();
        $nodes[0][''];
        return [
            [$nodes[4], ['file', 'line', 'function', 'class', 'type', 'args']]
        ];
    }

    public static function provideBuiltInStringArrFuncs()
    {
        $modTypes = ['passengers', 'segments', 'fareBasis', 'validatingCarrier'];
        $uniqueModTypes = array_unique($modTypes);
        $typeToIndex = array_flip($uniqueModTypes);
        $indexToType = array_flip($typeToIndex);
        $typeToIndex2 = array_flip($indexToType);
        return [
            [$typeToIndex, ['passengers', 'segments', 'fareBasis', 'validatingCarrier']],
            [$typeToIndex2, ['passengers', 'segments', 'fareBasis', 'validatingCarrier']],
        ];
    }

    private static function getPnrSchema()
    {
        return new DictP([], [
            'recordLocator' => new StringP([], ['pattern' => '/^[A-Z0-9]{6}$/']),
            'passengers' => new ListP([], ['elemType' => new DictP([], [
                'lastName' => new StringP([], []),
                'firstName' => new StringP([], []),
            ])]),
            'itinerary' => new ListP([], ['elemType' => new DictP([], [
                'from' => new StringP([], []),
                'to' => new StringP([], []),
                'date' => new StringP([], []),
            ])]),
            'commission' => new DictP([], [
                'units' => new StringP([], []),
                'value' => new StringP([], []),
            ]),
        ]);
    }

    /** @param $pnr = ParamUtil::sample(static::getPnrSchema()) */
    public static function provideParamValidation($pnr)
    {
        // should suggest: 'recordLocator', 'passengers', 'itinerary'
        // should not suggest: 'elemType'
        return [
            [$pnr, ['recordLocator', 'passengers', 'itinerary', 'commission']],
            [$pnr['commission'], ['units', 'value']],
        ];
    }

    public function provideTripleDotInBuiltInCall()
    {
        $list = [];
        $args = [];
        $args[] = ['huj' => 123, 'pizda' => 456];
        $args[] = ['zalupa' => 432, 'dzhigurda' => 523];
        $args[] = ['guzno' => 432, 'zhopa' => 523];
        $flat = array_merge(...$args);
        $flat[''];
        $list[] = [$flat, ['huj', 'pizda', 'zalupa', 'dzhigurda', 'guzno', 'zhopa']];
        return $list;
    }

    public function provideStaticInference()
    {
        $list = [];
        $arr = StaticInferenceChild::getArray();
        $arr[''];
        $list[] = [$arr, ['subKey']];
        $schema = StaticInferenceChild::getCompleteSchema();
        $schema[''];
        $list[] = [$schema, ['recordLocator', 'gds', 'isComplete']];
        return $list;
    }

    /** @param $heroes = [
     *     'reimu' => new ReimuHakurei,
     *     'ran' => new YakumoRan,
     * ] */
    public function providePhpDocNewNoNs($heroes)
    {
        $demand = $heroes['reimu']->demandDonuts();
        $shiki = $heroes['ran']->getFreeShikigami();
        $demand[''];
        $list[] = [$demand, ['patience', 'amount', 'consequences']];
        $list[] = [$shiki, ['name', 'power']];
        return $list;
    }

    private static function getBeer()
    {
        return [
            'price' => '1.79',
            'volume' => '0.5l',
            'alcohol' => '5.3%',
            'name' => 'Valmiermuizas',
            'quality' => 'good',
        ];
    }

    /** @param $beer = self::getBeer() */
//    /** @param $beer = UnitTest::getBeer() */
    public function provideSelfInPhpDoc($beer)
    {
        $beer[''];
        $list[] = [$beer, ['price', 'volume', 'alcohol', 'name', 'quality']];
        return $list;
    }

    private static function addAgeToPaxes($first, ...$paxes)
    {
        return array_map(function($pax){
            $pax['age'] = 25;
            return $pax;
        }, $paxes);
    }

    public function provideTripleDotInDecl()
    {
        $list = [];
        $vova = ['name' => 'Vova', 'country' => 'US'];
        $petja = ['name' => 'Petja', 'country' => 'GB'];
        $vasja = ['name' => 'Vasja', 'country' => 'FR', 'optData' => ['children' => 3, 'wives' => 2]];
        $paxesWithAge = static::addAgeToPaxes($vova, $petja, $vasja);
        $paxesWithAge[0][''];
        // should not suggest 'wives' and 'children' btw
        $list[] = [$paxesWithAge[0], ['name', 'country', 'optData', 'age']];
        return $list;
    }

    private static function addNumToSegments($segA, $segB, $segC, $segD, $segE)
    {
        return array_map(function($seg){
            $seg['segmentNumber'] = 123;
            return $seg;
        }, [$segA, $segB, $segC, $segD, $segE]);
    }

    public function provideTripleDotInCall()
    {
        $list = [];
        $segs = [];
        $segs[] = ['from' => 'RIX', 'to' => 'KIV'];
        $segs[] = ['from' => 'JFK', 'to' => 'LON'];
        $withNums = static::addNumToSegments(...$segs, ...$segs);
        $withNums[0][''];
        $list[] = [$withNums[0], ['from', 'to', 'segmentNumber']];
        return $list;
    }

    public function provideBracketExpression()
    {
        $dict = ['vova' => 1, 'misha' => 2];
        $asd = ($dict ?? null);
        $asd[''];
        $list[] = [$asd, ['vova', 'misha']];
        return $list;
    }

    public function provideArrayMapKeys()
    {
        $dict = [
            'signCityCode' => ' QSY',
            'pcc' => 'KL34 ',
            'airline' => '',
            'originType' => 'agency',
            'teamInitials' => '',
        ];
        $clean = array_map(function($match) { return trim($match) ?: null; }, $dict);
        $clean[''];
        $list[] = [$clean, ['signCityCode', 'pcc', 'airline', 'originType', 'teamInitials']];
        return $list;
    }

    private static function getPrivatePqs()
    {
        for ($i = 0; $i < 10; ++$i) {
            yield ['isPrivate' => false, 'price' => '100.00'];
        }
    }

    private static function getPublishedPqs()
    {
        for ($i = 0; $i < 10; ++$i) {
            yield ['isPrivate' => true, 'price' => '150.00'];
        }
    }

    // =============================
    //  control flow operator-related stuff follows
    // =============================

    public function provideTypeLostInElseScope($gds)
    {
        $list = [];

        $resultRecord = ['defaultKey' => 'asdsad'];
        if ($gds === 'apollo') {
            $resultRecord = ['pricingList' => [
                ['hop' => 'lej', 'lala' => 'lej'],
            ]];
        } elseif ($gds === 'sabre') {
            $resultRecord = ['pricingList' => [
                ['gde' => 'vopros', 'agde' => 'otver'],
            ]];
        } else if ($gds === 'amadeus') {
            $resultRecord = ['error' => 'Amadeus not supported yet'];
            if (rand(0, 15) < 7) {
                $resultRecord = ['nestedScopeInANestedScope' => [1,2,3]];
            }
        } else {
            throw new \Exception('Unsupported GDS - '.$gds);
        }
        if ($error = $resultRecord['error'] ?? null) {
            // completion works - correct
            $resultRecord[''];
            $list[] = [$resultRecord, ['defaultKey', 'pricingList', 'error', 'nestedScopeInANestedScope']];
            $list[] = [$resultRecord['pricingList'][0], ['hop', 'lala', 'gde', 'agde']];
        } else if (rand(0,10) > 5) {
            // completion does not work - incorrect, should fix
            $resultRecord[''];
            $list[] = [$resultRecord, ['defaultKey', 'pricingList', 'error', 'nestedScopeInANestedScope']];
            $list[] = [$resultRecord['pricingList'][0], ['hop', 'lala', 'gde', 'agde']];
        } elseif (rand(0,10) > 5) {
            // completion does not work - incorrect, should fix
            $resultRecord[''];
            $list[] = [$resultRecord, ['defaultKey', 'pricingList', 'error', 'nestedScopeInANestedScope']];
            $list[] = [$resultRecord['pricingList'][0], ['hop', 'lala', 'gde', 'agde']];
        } else {
            // completion does not work - incorrect, should fix
            $resultRecord[''];
            $list[] = [$resultRecord, ['defaultKey', 'pricingList', 'error', 'nestedScopeInANestedScope']];
            $list[] = [$resultRecord['pricingList'][0], ['hop', 'lala', 'gde', 'agde']];
        }
        return $list;
    }

    public function provideCompactInference()
    {
        $list = [];
        $age = 123;
        $height = 456;
        $book = ['pages' => 432, 'title' => 'Solaris'];
        $arr = compact('age', 'height', 'book', 'misspelledVar');
        $misspelledVar = [123,234];
        $arr[''];
        $arr['book'][''];
        $list[] = [$arr, ['age', 'height', 'book']];
        $list[] = [$arr['book'], ['pages', 'title']];
        return $list;
    }

    public function provideAssignmentOfAssignment()
    {
        $list = [];
        $whatToDrink = $booze = ['name' => 'Rum', 'spiritage' => '37.5%', 'taste' => 'normal'];
        $booze[''];
        $whatToDrink[''];
        $list[] = [$whatToDrink, ['name', 'spiritage', 'taste']];
        return $list;
    }

    private static function getPqs()
    {
        yield from static::getPrivatePqs();
        yield from static::getPublishedPqs();
    }

    public function provideYieldFrom()
    {
        $pqs = static::getPqs();
        $pqs[0][''];
        $list[] = [$pqs[0], ['isPrivate', 'price']];
        return $list;
    }

    /** @return array [
     *     'baseUrl' => 'http://midiana.lv',
     *     'isDevelopment' => true,
     *     'fluentdIp' => '128.0.0.1',
     *     'fluentdPort' => '468138',
     * ] */
    private static function getLocalConfig()
    {
        return json_decode(file_get_contents('/var/www/html/local_config.json'), true);
    }

    /** @return [
     *     'securityKey' => 'qwe123',
     *     'loggerIp' => '192.168.0.100',
     * ] */
    private static function getGlobalConfig()
    {
        return json_decode(file_get_contents('/var/www/html/global_config.json'), true);
    }

    /** @return = [
     *     'mailServiceUrl' => 'www.dev.mail.com',
     *     'apolloLogin' => 'student',
     * ] */
    private static function getDevConfig()
    {
        return json_decode(file_get_contents('/var/www/html/dev_config.json'), true);
    }

    /** @return array = [
     *     'mailServiceUrl' => 'www.prod.mail.com',
     *     'apolloLogin' => 'employee',
     * ] */
    private static function getProdConfig()
    {
        return json_decode(file_get_contents('/var/www/html/prod_config.json'), true);
    }

    public function provideReturnDoc()
    {
        $config = static::getLocalConfig();
        $config['fluentdPort'];
        $list[] = [$config, ['baseUrl', 'isDevelopment', 'fluentdIp', 'fluentdPort']];
        $config2 = static::getGlobalConfig();
        $config2[''];
        $config3 = static::getDevConfig();
        $config3[''];
        $config4 = static::getProdConfig();
        $config4[''];
        $list[] = [$config, ['baseUrl', 'isDevelopment', 'fluentdIp', 'fluentdPort']];
        $list[] = [$config2, ['securityKey', 'loggerIp']];
        $list[] = [$config3, ['mailServiceUrl', 'apolloLogin']];
        $list[] = [$config4, ['mailServiceUrl', 'apolloLogin']];
        return $list;
    }

    private static function getFareStream()
    {
        for ($i = 0; $i < 10; ++$i) {
            yield [
                'fareBasis' => 'QWE123',
                'currency' => 'USD',
                'airline' => 'AA',
                'amount' => rand(1, 100),
            ];
        }
    }

    public function provideYield()
    {
        $fares = [];
        foreach (static::getFareStream() as $i => $fare) {
            $fare['index'] = $i;
            $fares[] = $fare;
        }
        $fares[0][''];
        $list[] = [$fares[0], ['fareBasis', 'currency', 'airline', 'amount', 'index']];
        return $list;
    }

    private static function getGrabFaresScheme()
    {
        return new DictP([], [
            'cnt' => 123,
            'pcc' => 'KLS3',
            'cmd' => '$D10MAYKIVRIX',
        ]);
    }

    /** @param $params = ExactKeysUnitTest::getGrabFaresScheme() */
    private static function provideNewWoBgTyping($params)
    {
        $list = [];
        $sampleData = null;
        // recursion caused type from doc be lost cuz
        // i took first resolution instead of all
        if ($params instanceof DictP) {
            foreach ($params->definition as $key => $val) {
                $sampleData[$key] = static::provideNewWoBgTyping($val);
            }
        } else {
            $sampleData = $params;
        }
        $list[] = [$sampleData, ['cnt', 'pcc', 'cmd']];
        $list[] = [$params->definition, ['cnt', 'pcc', 'cmd']];
        return $list;
    }

    public static function provideObjectInAKeyMethod()
    {
        $list = [];

        $colorToNum = [];
        $zhopa = ['black', 'yellow', 'green'];
        foreach ($zhopa as $color) {
            $colorToNum[$color] = rand(0,100);
        }
        $colorToNum['green'];

        $storRecord = [
            'capacity' => '256mb',
            'path' => '/home/klesun/person_storage.db',
            'stor' => new PersonStorage(),
        ];
        $added = $storRecord['stor']->addPerson();
        $added[''];
        $list[] = [$added, ['status', 'spaceLeft']];
        return $list;
    }

    public function provideArrayAdd()
    {
        $humanRights = [
            'toTrust' => 'your friends',
            'toLove' => 'your family',
            'toHate' => 'your chef',
        ];
        $citizenRights = [
            'toAttend' => 'school',
            'toVote' => 'for anyone bust honest guys',
            'toPreserveSilence' => 'when there is nothing to say',
        ];
        $merged = $humanRights + $citizenRights;
        $merged[''];
        $list[] = [$merged, ['toTrust', 'toLove', 'toHate', 'toAttend', 'toVote', 'toPreserveSilence']];
        return $list;
    }

    private static function addPax($seg)
    {
        $seg['pax'] = 'Marina Libermane';
        return $seg;
    }

    public function provideGenericsInFuncVar()
    {
        $list = [];

        $addFullDt = function($seg) {
            $seg['fullDt'] = date('Y-m-d H:i:s');
            return $seg;
        };

        $seg = ['from' => 'TYO', 'to' => 'ROB'];
        $seg = $addFullDt($seg);
        $seg[''];

        $itinerary = [
            ['from' => 'MOW', 'to' => 'CDG'],
            ['from' => 'CDG', 'to' => 'LAX'],
        ];
        $datedItin = array_map($addFullDt, $itinerary);
        $datedItin[0][''];

        $paxedItin = array_map([self::class, 'addPax'], $datedItin);
        $paxedItin[0][''];

        $list[] = [$seg, ['from', 'to', 'fullDt']];
        $list[] = [$datedItin[0], ['from', 'to', 'fullDt']];
        $list[] = [$paxedItin[0], ['from', 'to', 'fullDt', 'pax']];

        return $list;
    }

    private static function providePrivateFuncArgFromArrayMap($person)
    {
        $list = [];
        $person[''];
        $list[] = [$person, ['height', 'gender', 'dream']];
        return $list;
    }

    public function executePersonMapping()
    {
        $persons = [
            ['height' => 181, 'gender' => 'male', 'dream' => 'a can of beer'],
            ['height' => 161, 'gender' => 'female', 'dream' => 'great wedding'],
        ];
        return array_map([static::class, 'providePrivateFuncArgFromArrayMap'], $persons);
    }

    public static function providePseudoPrivateFuncArgCompletion($pccRows)
    {
        $list = [];
        $list[] = [$pccRows[0], ['pcc', 'is_tour']];
        return $list;
    }

    public function executePccReprice()
    {
        $result = static::providePseudoPrivateFuncArgCompletion([[
            'pcc' => 'KL33',
            'is_tour' => true,
        ], [
            'pcc' => 'KL35',
            'is_tour' => false,
        ]]);
    }

    private function providePrivatePeekOutside(array $pax)
    {
        $list = [];
        $pax['']; // should suggest keys determined from usage
        $list[] = [$pax, ['name', 'salary', 'experience']];
        return $list;
    }

    public function parseDenyaEmployee(array $pax)
    {
        $pax = ['name' => 'Denya', 'salary' => '15.00', 'experience' => '89%'];
        $provided = $this->providePrivatePeekOutside($pax);
    }

    private function providePrivateIndirectPeekOutside(array $pax)
    {
        $list = [];
        $pax['']; // should suggest keys determined from usage
        $list[] = [$pax, ['name', 'salary', 'experience']];
        return $list;
    }

    public function parseManyDenyaEmployees(array $pax)
    {
        $paxes = [
            ['name' => 'Denya', 'salary' => '15.00', 'experience' => '89%'],
            ['name' => 'Ilja', 'salary' => '15.50', 'experience' => '91%'],
        ];
        $transformed = array_map([self::class, 'providePrivateIndirectPeekOutside'], $paxes);
    }

    private static function makeFcSegment($segmentData, $fareInfo)
    {
        return array_merge($segmentData, $fareInfo);
    }

    public function provideInlineVarDoc($db)
    {
        $list = [];
        /** @var array $arr = ['key' => 'val'] */
        $arr['']; // should suggest: key
        $list[] = [$arr, ['key']];

        $options = $db->getFromTableById('module_storage', 123);
        /** @var array $options = MyModuleOptions::get() */
        $options['']; // should suggest: dependencies, description, license, version
        $list[] = [$options, ['dependencies', 'description', 'license', 'version']];
        return $list;
    }

    public function provideInlineVarDocAboveVar($db)
    {
        $list = [];
        /** @var array $options = MyModuleOptions::get() */
        $options = $db->getFromTableById('module_storage', 123);
        $options['']; // should suggest: dependencies, description, license, version
        $list[] = [$options, ['dependencies', 'description', 'license', 'version']];
        return $list;
    }

    public function provideCallUserFuncArray()
    {
        $args = [
            ['from' => 'LON', 'to' => 'TYO'],
            ['fare' => '150.00', 'fareBasis' => 'CHD50AD'],
        ];
        $fcSegment = call_user_func_array([static::class, 'makeFcSegment'], $args);
        $fcSegment;
        $list[] = [$fcSegment, ['from', 'to', 'fare', 'fareBasis']];
        return $list;
    }

    private static function addAirlineToSsrs()
    {
        $args = func_get_args();
        $arg = func_get_arg(1);
        $addAir = function($ssr){
            $ssr['airline'] = 'BT';
            return $ssr;
        };
        return [
            'all' => array_map($addAir, $args),
            'third' => $addAir($arg),
        ];
    }

    public function provideFuncGetArgs()
    {
        $list = [];
        $ssrs = self::addAirlineToSsrs(
            ['code' => 'WCHR', 'line' => 5],
            ['code' => 'KSML', 'line' => 6]
        );
        $list[] = [$ssrs['all'][0], ['code', 'line', 'airline']];
        $list[] = [$ssrs['third'], ['code', 'line', 'airline']];
        return $list;
    }

    // =============================
    //  SQL related stuff follows
    // =============================

    public function providePdoSqlLowerCaseSelect()
    {
        $list = [];
        $connection = new \PDO();
        $sql = 'select id, cmd_performed, dt from terminal_command_log order by id desc limit 1;';
        $stmt = $connection->query($sql);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'cmd_performed', 'dt']];
        return $list;
    }

    public function providePdoSqlSelect()
    {
        $list = [];
        $connection = new \PDO();
        $sql = 'SELECT id, cmd_performed, dt FROM terminal_command_log ORDER BY id DESC limit 1;';
        $stmt = $connection->query($sql);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'cmd_performed', 'dt']];
        return $list;
    }

    public function providePdoSqlSelectWithAs($params)
    {
        $list = [];
        $connection = new \PDO();
        $sql = 'SELECT cmd_performed, COUNT(*) AS cnt, max(dt) as max_dt FROM terminal_command_log ORDER BY id DESC limit 1;';
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['cmd_performed', 'cnt', 'max_dt']];
        return $list;
    }

    public function providePdoSqlSelectMultiTable($params)
    {
        $list = [];
        $connection = new \PDO();
        $sql = 'SELECT tcl.id, tcl.cmd_performed, ts.agent_id FROM terminal_command_log tcl JOIN  terminal_sessions ts ON ts.id = tcl.session_id ORDER BY id DESC limit 1;';
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'cmd_performed', 'agent_id']];
        return $list;
    }

    public function providePdoSqlSelectConcatSql($params)
    {
        $list = [];
        $connection = new \PDO();
        $sql =
            'SELECT tcl.id, tcl.cmd_performed, ts.agent_id'.PHP_EOL.
            'FROM terminal_command_log tcl'.PHP_EOL.
            'JOIN terminal_sessions ts ON ts.id = tcl.session_id'.PHP_EOL.
            'ORDER BY id DESC limit 1;'.PHP_EOL;
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'cmd_performed', 'agent_id']];
        return $list;
    }

    public function providePdoSqlSelectImplodeSql($params)
    {
        $list = [];
        $connection = new \PDO();
        $sql = implode(PHP_EOL, [
            'SELECT tcl.id, tcl.cmd_performed, ts.agent_id',
            'FROM terminal_command_log tcl',
            'JOIN terminal_sessions ts ON ts.id = tcl.session_id',
            'ORDER BY id DESC limit 1;',
        ]);
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'cmd_performed', 'agent_id']];
        return $list;
    }

    public function providePdoSqlSelectLongColNamesSql($params)
    {
        $list = [];
        $connection = new \PDO();
        $sql = implode(PHP_EOL, [
            'SELECT',
            '    GROUP_CONCAT( tcl.cmd_performed) AS cmds,',
            '    ts.id AS session_id,',
            '    ts.created_dt,',
            '    ts.agent_id',
            'FROM terminal_command_log tcl',
            'JOIN terminal_sessions ts ON ts.id = tcl.session_id',
            'GROUP BY ts.id',
            'ORDER BY id DESC limit 1;',
        ]);
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['cmds', 'agent_id', 'created_dt', 'session_id']];
        return $list;
    }

    public function provideSelectAll($params)
    {
        $list = [];
        $connection = new \PDO();
        // don't forget to run upgrade_sql.sql and setup your DB in _Database_ window
        $sql = 'SELECT * FROM delete_me;';
        $stmt = $connection->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch(\PDO::FETCH_ASSOC);
        $result[''];
        $list[] = [$result, ['id', 'name', 'price']];
        return $list;
    }

    public function provideOptionChain()
    {
        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})->unwrap()[''];

        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})
            ->filter(function($value4){return $value4[''];});

        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})
            ->map(function($value2){return $value2 + ['c' => 3];})
            ->map(function($value3){return $value3 + ['d' => 4];})
            ->filter(function($value4){return $value4[''];})
            ->map(function($value4){return $value4 + ['e' => 5];})
            ->map(function($value4){return $value4 + ['f' => 5];})
            ->map(function($value4){return $value4 + ['g' => 5];})
            ->map(function($value4){return $value4 + ['h' => 5];})
            ->map(function($value4){return $value4 + ['i' => 5];})
            ->map(function($value4){return $value4 + ['j' => 5];})
            ->map(function($value4){return $value4 + ['k' => 5];})
            ->map(function($value4){return $value4 + ['l' => 5];})
            ->map(function($value4){return $value4 + ['m' => 5];})
            ->map(function($value4){return $value4 + ['n' => 5];})
            ->map(function($value4){return $value4 + ['o' => 5];})
            ->map(function($value4){return $value4 + ['p' => 5];})
            ->map(function($value4){return $value4 + ['q' => 5];})
            ->map(function($value4){return $value4 + ['r' => 5];})
            ->filter(function($value4){return $value4[''];})
            ->map(function($value4){return $value4 + ['s' => 5];})
//            ->map(function($value4){return $value4 + ['t' => 5];})
//            ->map(function($value4){return $value4 + ['u' => 5];})
//            ->map(function($value4){return $value4 + ['v' => 5];})
//            ->map(function($value4){return $value4 + ['w' => 5];})
//            ->map(function($value4){return $value4 + ['x' => 5];})
//            ->map(function($value4){return $value4 + ['y' => 5];})
//            ->map(function($value4){return $value4 + ['z' => 5];})
//            ->map(function($value4){return $value4 + ['a1' => 5];})
//            ->map(function($value4){return $value4 + ['a2' => 5];})
        ;
        $result->result[''];
        $result->unwrap()[''];
        $list[] = [$result->unwrap(), [
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            // 't', 'u', 'v', 'w', 'x', 'y', 'z', 'a1', 'a2'
        ]];
        return $list;
    }

    public static function provideUnknownStringConcatenation()
    {
        $xmlParsed = [
            'soap:Envelope' => [
                [
                    'soap:Header' => [],
                    'soap:Body' => [
                        [
                            'soap:Fault' => [
                                [
                                    'faultcode' => '1253435',
                                    'faultstring' => 'Invalid or expired session token',
                                ],
                            ],
                        ],
                    ],
                ],
            ],
        ];
        $fullRootName = array_keys($xmlParsed)[0];
        $tuple = explode(':', $fullRootName);
        $nsPrefix = count($tuple) === 2 ? $tuple[0] : '';
        //$soapFault = $xmlParsed['soap:Envelope'][0]['soap:Body'][0]['soap:Fault'][0] ?? null;
        $soapFault = $xmlParsed[$nsPrefix.'Envelope'][0][$nsPrefix.'Body'][0][$nsPrefix.'Fault'][0] ?? null;
        $soapFault[''];
        return [
            [$soapFault, ['faultcode', 'faultstring']],
        ];
    }

    private static function complicateArr(array $arr)
    {
        $arr = array_merge([], $arr);
        $arr = [] + $arr;
        $arr = [$arr][0];
        $arr = ['asd' => ['dsa' => $arr]]['asd']['dsa'];
        $arr = array_combine(array_keys($arr), array_values($arr));
        return $arr;
    }

    private static function getPnrData()
    {
        $pnrData = [
            'currentPricing' => [
                'parsed' => static::complicateArr([
                    'pricingList' => static::complicateArr([
                        static::complicateArr(['netPrice' => '200.00', 'currency' => 'USD']),
                        static::complicateArr(['netPrice' => '200.00', 'currency' => 'USD']),
                        static::complicateArr(['netPrice' => '200.00', 'currency' => 'USD']),
                    ]),
                ]),
            ],
        ];
        if (rand() % 2) {
            $pnrData = static::complicateArr($pnrData);
        }
        if (rand() % 2) {
            return ['error' => 'You are not lucky ;c'];
        } else {
            return ['pnrData' => $pnrData];
        }
    }

    /** @param $pnrData = static::getPnrData()['pnrData'] */
    public static function provideForeachKeyLimitedResolutionCachingBug($pnrData)
    {
        // I still could not reproduce it actually without copying the whole
        // project, but this test should cover some good corner cases nevertheless
        foreach ($pnrData['currentPricing']['parsed']['pricingList'] as $i => $store) {
            $pnrData['currentPricing']['parsed']['pricingList'][$i] = static::complicateArr($store);
        }
        foreach ($pnrData['currentPricing']['parsed']['pricingList'] as $i => $store) {
            $pnrData['currentPricing']['parsed']['pricingList'][$i] = static::complicateArr($store);
        }
        $pnrData['currentPricing']['parsed']['pricingList'][0]['netPrice'];
        return [
            [$pnrData, ['currentPricing']],
            [$pnrData['currentPricing'], ['parsed']],
            [$pnrData['currentPricing']['parsed'], ['pricingList']],
            [$pnrData['currentPricing']['parsed']['pricingList'][0], ['netPrice', 'currency']],
        ];
    }

    public static function provideArrayFillKeys()
    {
        $row = array_fill_keys(['id', 'name', 'price', 'currency'], '');
        $row[''];
        $list[] = [$row, ['id', 'name', 'price', 'currency']];

        $lreplace = array_replace(['a' => '', 'b' => ''], ['a' => 5, 'b' => 6, 'c' => 7]);
        $list[] = [$lreplace, ['a', 'b', 'c']];

        $rreplace = array_replace(['a' => '', 'b' => '', 'c' => ''], ['a' => 5, 'b' => 6]);
        $list[] = [$rreplace, ['a', 'b', 'c']];

        return $list;
    }

    public static function provideClassInAVar()
    {
        $cls = PersonStorage::class;
        $obj = new $cls();
//        $obj = new PersonStorage();
        $result = $obj->addPerson('Vova', 18);
        $result[''];

        $fields = $cls::getFields();
        $fields[''];

        return [
            [$result, ['status', 'spaceLeft']],
            [$fields, ['markup', 'name', 'price']],
        ];
    }

    private static function combineTicketsAndInvoices(array $tickets, array $invoices)
    {
        $invoices[0]['invoiceNumber'];
        $numToInv = array_combine(array_column($invoices, 'ticketNumber'), $invoices);
        return array_map(function($ticket)use($numToInv){
            $numToInv['0123456789012']['invoiceNumber'];
            $ticket['ticketInvoiceInfo'] = $numToInv[$ticket['ticketNumber']] ?? null;
            $ticket['ticketInvoiceInfo'][''];
            return $ticket;
        }, $tickets);
    }

    public static function provideClosureUse()
    {
        $tickets = [
            ['ticketNumber' => '0123456789012', 'name' => 'Vasya/Pupkin'],
            ['ticketNumber' => '0123456789013', 'name' => 'Vasita/Pupkina'],
        ];
        $invoices = [
            ['ticketNumber' => '0123456789012', 'invoiceNumber' => '13412314', 'isVoided' => false],
            ['ticketNumber' => '0123456789013', 'invoiceNumber' => '13412315', 'isVoided' => false],
        ];
        $combined = static::combineTicketsAndInvoices($tickets, $invoices);
        $combined[0]['ticketInvoiceInfo']['ticketNumber'];
        return [
            [$combined[0], ['ticketNumber', 'name', 'ticketInvoiceInfo']],
            [$combined[0]['ticketInvoiceInfo'], ['ticketNumber', 'invoiceNumber', 'isVoided']],
        ];
    }

    private static function getLocations()
    {
        return [
            ['type' => 'city', 'value' => 'WAS'],
            ['type' => 'airport', 'value' => 'JFK'],
            ['type' => 'country', 'value' => 'LV'],
            ['type' => 'region', 'value' => '34'],
        ];
    }

    /** @param $rule = [
     *     'departure_items' => static::getLocations(),
     *     'destination_items' => static::getLocations(),
     * ] */
    private static function normalizeRule(array $rule, bool $forClient)
    {
        // remove empty condition keys to make field more readable in db cli
        $normArr = !$forClient
            ? function(array $arr){return array_filter($arr);}
            : function(array $arr){return $arr;};

        return $normArr([
            'departure_items' => array_map(function($item) use ($forClient,$normArr){
                return $normArr([
                    'type' => strval($item['type']),
                    'value' => strval($item['value']),
                    'name' => $forClient ? static::getLocationName($item) : null,
                ]);
            }, $rule['departure_items'] ?? []),
            'destination_items' => array_map(function($item) use ($forClient,$normArr){
                return $normArr([
                    'type' => strval($item['type']),
                    'value' => strval($item['value']),
                    'name' => $forClient ? static::getLocationName($item) : null,
                ]);
            }, $rule['destination_items'] ?? []),
            'reprice_pcc_records' => array_map(function($item) use ($forClient,$normArr){
                return $normArr([
                    'gds' => strval($item['gds']),
                    'pcc' => strval($item['pcc']),
                    'ptc' => strval($item['ptc'] ?? ''),
                    'account_code' => strval($item['account_code'] ?? ''),
                    'fare_type' => strval($item['fare_type'] ?? ''),
                ]);
            }, $rule['reprice_pcc_records'] ?? []),
        ]);
    }

    public function provideUseInUseInUse($dataStr)
    {
        $rule = self::normalizeRule(json_decode($dataStr, true), false);
        $rule['destination_items'][0]['type'] === '';
        $types = array_column($rule['destination_items'], 'type');
        return [
            [$rule, ['departure_items', 'destination_items', 'reprice_pcc_records']],
            [$rule['departure_items'][0], ['type', 'value', 'name']],
            [$rule['destination_items'][0], ['type', 'value', 'name']],
            [$rule['reprice_pcc_records'][0], ['gds', 'pcc', 'ptc', 'account_code', 'fare_type']],
            [array_flip($types), ['city', 'airport', 'country', 'region']]
        ];
    }

    /**
     * wrap private method in a closure to
     * allow passing it outside of this class
     */
    private static function closed(callable $meth)
    {
        // this could be moved to a helper class - will
        // need to use reflection to call private method
        return function(...$args) use ($meth) {return $meth(...$args);};
    }

    private static function parseSegment($line)
    {
        return [
            'lineNumber' => 123,
            'from' => 'KIV',
            'to' => 'RIX',
            'date' => '2018-02-15',
        ];
    }

    public function provideMethWrapper()
    {
        $lines = [
            ' ASD D 1BJ JK J 2J 2J ',
            ' ASD 2INCI2 I 2IC2IO  ',
            ' A3G3D 1BS JK J 2J 2J ',
        ];
        $segments = Fp::map(self::closed([self::class, 'parseSegment']), $lines);
        return [
            [$segments[0], ['lineNumber', 'from', 'to', 'date']],
        ];
    }

    public function providePropDocComment()
    {
        $storage = new PersonStorage();

        // from @method doc tag
        $flushed = $storage->flush(20000);
        $flushed['affected_rows'];
        $info = $storage->info();
        $info[''];

        // from @property doc tag
        $reimu = $storage->reimuResult;
        $storage->pnrData[''];
        $reimu->unwrap()->demandDonuts()[''];

        return [
            [$storage->pnrData, ['reservation', 'currentPricing']],
            [$storage->pnrData['currentPricing'], ['pricingList']],
            [$reimu->unwrap()->demandDonuts(), ['patience', 'amount', 'consequences']],
            [$flushed, ['status', 'affected_rows']],
            [$info, ['connections', 'uptime_seconds']],
        ];
    }

    public function provideIntOverflow()
    {
        $numSet = array_flip([4294967295, 123, -4294967295, 4294967295232323231313123123]);
        $numSet[''];
        return [
            [$numSet, ['4294967295', '123', '4294967295232323231313123123']],
        ];
    }

    public function provideCast()
    {
        $developer = ['skill' => 9.8, 'salary' => '500.00', 'age' => 23];
        $objDev = (object)$developer;
        // should suggest: skill, salary, age
        $objDev->s;
        $castedDev = (array)$objDev;
        $castedDev[''];
        $qualification = ['frontend', 'backend', 'fullstack'][rand()];
        $castedQual = (string)$qualification;
        $number = [42, 1337, 228][rand()];
        $castedNum = (int)$number;
        return [
            [$castedDev, ['skill', 'salary', 'age']],
            [array_flip([$castedQual]), ['frontend', 'backend', 'fullstack']],
            [array_flip([$castedNum]), ['42', '1337', '228']],
        ];
    }

    public function provideMagicMethodInference()
    {
        $object = new SomeCls123();
        // should suggest: database, max_occurences
        $object->a;
        // should suggest: server, user, pass, database, port, socket
        $object->database->e;
        return [
            [(array)$object->database, ['server', 'user', 'pass', 'database', 'port', 'socket']],
        ];
    }

    private function getPropByName($name)
    {
        $data = (object)[
            'drinks' => [
                ['name' => 'pepper', 'price' => 'tasty'],
                ['name' => 'schweps', 'price' => 'not bad'],
            ],
            'foods' => [
                ['calories' => 'many', 'cooking_time' => 'fast'],
                ['calories' => 'not so many', 'cooking_time' => 'slow'],
            ],
        ];
        $data->;
        return $data->{$name};
    }

    public function provideVarNameProperty()
    {
        $agent = (object)['id' => 123, 'name' => 'vova',
            'company' => ['domain' => 'CRA', 'tz' => 'America/New_York'],
            'team' => ['motto' => 'get sales or get lost!', 'auditor' => 'Boris'],
        ];
        $agent->company[''];
        $companyPropName = 'company';
        $company = $agent->{$companyPropName};
        $company[''];
        $teamPropName = 'team';
        $team = $agent->$teamPropName;
        $team[''];

        $propName = 'database';
        $dbRec = SomeCls123::$data->$propName;
        $zhopa = SomeCls123::getProp('zhopa');
        $zhopa->u;

        return [
            [(array)$company, ['domain', 'tz']],
            [(array)$team, ['motto', 'auditor']],
            [(array)$dbRec, ['server','user','pass','database','port','socket']],
            [(array)$zhopa, ['guzno','dzhigurda']],
        ];
    }

    /**
     * @param array[][]  $params = require __DIR__ . '../config/params.php'
     */
    public function provideReturnInRequire($params)
    {
        $params[''];
        $real = require __DIR__ . '/../config/params.php';
        $real[''];
        return [
            [$real, ['value1', 'value2', 'value3']],
            [$params, ['value1', 'value2', 'value3']],
        ];
    }

    private function getMysqli()
    {
        return new \mysqli("10.128.128.150", "stasadm", "G0a4wa&", "rbstools");
    }

    private function getMysqliProcedural()
    {
        return mysqli_connect("10.128.128.150", "stasadm", "G0a4wa&", "rbstools");
    }

    private function queryMysqli()
    {
        $mysqli = $this->getMysqli();
        return $mysqli->query("SELECT id, iata_code, name FROM airports limit 20;");
    }

    private function queryMysqliProcedural()
    {
        $mysqli = $this->getMysqliProcedural();
        return mysqli_query($mysqli, "SELECT id, iata_code, name FROM airports limit 20;");
    }

    public function provideMysqliIterator()
    {
        $rows = [];
        $result = $this->queryMysqli();
        foreach ($result as $row) {
            $rows[] = $row;
        }
        $rows[0][''];
        return [
            [$rows[0], ['id', 'iata_code', 'name']],
        ];
    }

    public function provideMysqliIteratorProcedural()
    {
        $rows = [];
        $result = $this->queryMysqliProcedural();
        foreach ($result as $row) {
            $rows[] = $row;
        }
        $rows[0][''];
        return [
            [$rows[0], ['id', 'iata_code', 'name']],
        ];
    }

    public function provideMysqliFetchAssoc()
    {
        $result = $this->queryMysqli();
        $row = $result->fetch_assoc();
        $row[''];
        return [
            [$row, ['id', 'iata_code', 'name']],
        ];
    }

    public function provideMysqliFetchAssocProcedural()
    {
        $result = $this->queryMysqliProcedural();
        $row = mysqli_fetch_assoc($result);
        $row[''];
        return [
            [$row, ['id', 'iata_code', 'name']],
        ];
    }

    public function provideMysqliFetchAll()
    {
        $result = $this->queryMysqli();
        $intRows = $result->fetch_all();
        $rows = $result->fetch_all(MYSQLI_ASSOC);
        $rows[0][''];
        return [
            [$intRows[0], []],
            [$rows[0], ['id', 'iata_code', 'name']],
        ];
    }

    public function provideMysqliFetchAllProcedural()
    {
        $result = $this->queryMysqliProcedural();
        $intRows = mysqli_fetch_all($result);
        $rows = mysqli_fetch_all($result, MYSQLI_ASSOC);
        $rows[''];
        return [
            [$intRows[0], []],
            [$rows[0], ['id', 'iata_code', 'name']],
        ];
    }

    /** @param $reservation = self::getReservation() */
    public static function transformReservation($reservation)
    {
        $reservation['passengers'] = [];
        $reservation['passengers'] = array_map(function($a){return ['lastName' => 'Pupkin'];}, $reservation['passengers']);
        return $reservation;
    }

    public static function getReservation()
    {
        // must pass some args, won't reproduce otherwise
        return self::transformReservation($a);
    }

    public function provideArrayMapInfRec()
    {
        $imported = self::getReservation();
        $imported['passengers'][0][''];
        return [
            [$imported['passengers'][0], ['lastName']],
        ];
    }

    public function provideArrayColumnObj()
    {
        $arrSups = [
            ['a' => ['subA1' => 3, 'subA2' => 8], 'b' => 6],
            ['a' => ['subA1' => 12, 'subA2' => 18], 'b' => 2],
        ];
        $arrSubs = array_column($arrSups, 'a');
        $list[] = [$arrSubs[0], ['subA1', 'subA2']];

        $stdSups = [
            (object)['a' => ['subA1' => 3, 'subA2' => 8], 'b' => 6],
            (object)['a' => ['subA1' => 12, 'subA2' => 18], 'b' => 2],
        ];
        $stdSubs = array_column($stdSups, 'a');
        $stdSubs[0][''];
        $list[] = [$stdSubs[0], ['subA1', 'subA2']];

        $instSups = [new Ksha(), new Ksha(), new Ksha()];
        $instSubs = array_column($instSups, 'weapons');
        $instSubs[0][''];
        $list[] = [$instSubs[0], ['pistols', 'micro-smgs', 'assault-rifles']];

        return $list;

    }

    private function importAgents()
    {
        $rawAgents = [
            ['id' => 123, 'login' => 'Vasya'],
            ['id' => 124, 'login' => 'Petya'],
            ['id' => 125, 'login' => 'Dima'],
            ['id' => 126, 'login' => 'Gosha'],
        ];
        $normAgents = array_map([$this, 'normalizeAgent'], $rawAgents);
        file_put_contents('agents.json', json_encode($normAgents));
    }

    private function normalizeAgent($object)
    {
        $object['normalized'] = true;
        return $object;
    }

    /** @param $agents = [self::normalizeAgent()] */
    public function provideEmptyArgsDoc($agents)
    {
        // even though normalizeAgent() is called without args in the doc, we
        // should treat it as "unknown args", since that' more convenient
        return [
            [$agents[0], ['id', 'login', 'normalized']],
        ];
    }

    /** @param $jsData = at('index.js').makeCustomer() */
    public function provideJsVarInDoc($jsData)
    {
        $jsData[''];
        $list[] = [$jsData, ['name', 'money', 'loyalty', 'friends', 'sales']];
        $list[] = [$jsData['friends'][0], ['name', 'money', 'loyalty', 'friends', 'sales']];
        $jsData['sales'][0][''];
        $list[] = [$jsData['sales'][0], ['dt', 'product', 'price']];
        return $list;
    }

    /** @param $data = require('someNonExistingFile.php') */
    public function provideRequireNotExisting($data)
    {
        $data[''];
        // should not cause IllegalArgumentException
        return [
            [$data + ['a' => 1], ['a']],
        ];
    }

    public function provideCorrectConstCompletion()
    {
        $cls = FooBar::class;
        ($cls::KEY_2)[''];
        ($cls::$KEY_2)[''];
        ($cls::prop)[''];
        ($cls::$prop)[''];
        return [
            // non-existing props, should not be defined by plugin
            [$cls::$KEY_1, []],
            [$cls::$KEY_2, []],
            [$cls::$KEY_3, []],
            // yes-existing consts, _should_ be defined by plugin
            [$cls::KEY_1, ['a']],
            [$cls::KEY_2, ['b']],
            [$cls::KEY_3, ['c']],
            // yes-existing prop, _should_ be defined by plugin
            [$cls::$prop, ['p']],
            // non-existing const, should not be defined by plugin
            [$cls::prop, []],
        ];
    }

    public function provideThisInMethDoc()
    {
        $user = new users_table();
        $user->getById(123)[''];
        $user->getAll()[0][''];
        $user->jsonSerialize()['nameRecord'][''];
        $user->paxTypes[0][''];
        return [
            [$user->getAll()[0], ['id', 'username']],
            [$user->jsonSerialize()['nameRecord'], ['lastName', 'firstName', 'number']],
            [$user->paxTypes[0], ['age', 'ptc', 'fareGroup', 'ageGroup']],
        ];
    }

    public function provideRepeatingFqn()
    {
        $period = [
            'period' => new \DatePeriod(
                new \DateTimeImmutable('now'),
                new \DateInterval('P0Y'),
                new \DateTimeImmutable('P3DAY')
            ),
            'epicBattle' => new \Dmc\EpicBattle(),
        ];
        // should suggest \Datetime methods
        $period['period']->end->;
        // should suggest "getWeapons"
        $period['epicBattle']->protag->;
        $weapons = $period['epicBattle']->participants[0]->getWeapons();
        return [
            [$period['epicBattle']->protag->getWeapons(), ['beowulf', 'yamato', 'rebellion']],
            [$weapons, ['beowulf', 'yamato', 'rebellion']],
        ];
    }

    public function provideTypeFromGLOBALS()
    {
        global $ololo1223_tanya_the_evil, $ololo1223_date, $ololoNoMagic;
        // should suggest: age, gender, abilities
        $ololo1223_tanya_the_evil[''];
        // should suggest \DateTime methods
        $ololo1223_date->;
        return [
            [$ololo1223_tanya_the_evil, ['age', 'gender', 'abilities']],
            [$ololoNoMagic, ['base_url', 'db_password', 'redis_password']],
        ];
    }

    public function provideMetaDefMeth()
    {
        $struct = (new \Library\Book())->getStruct();
        $purchased = (new \Library\Book())->purchase([]); // from interface
        $read = (new \Library\Book())->read([]); // from extended class
        $read['bytesChunk'];
        return [
            [$struct, ['title', 'author', 'chapters']],
            [$purchased, ['status', 'confirmationCode', 'transactionDt', 'serialKey']],
            [$read, ['connection', 'bytesChunk', 'bytesLeft']],
        ];
    }

    public function provideMetaDefFunc()
    {
        $gzipped = \gzip_by_dict([]);
        $gzipped['bytes'];
        return [
            [$gzipped, ['bytes', 'newDict']],
        ];
    }

    public function provideArgTypeFromMetaInsideFunc($juice)
    {
        $juice[''];
        $juice['fruit'];
        return [
            [$juice, ['fruit', 'fruitPercentage', 'pricePerLitre']],
        ];
    }

    public function provideInheritedFuncMetaFqn($param)
    {
        $result = (new City())->first('cities', $param);
        $result['retrieval_time'];
        return [
            [$result, ['retrieval_time', 'id', 'name']],
        ];
    }

    public function provideMetaArgSet()
    {
        $result = myGetOptions();
        $result[''];
        return [
            [$result, ['foo', 'bar', 'name', 'list', 'types', 'object', 'mode', 'flags']]
        ];
    }

    /** @return array{foo: string, bar: int} */
    private function getPsalmArray()
    {
        return json_decode(file_get_contents('asdsad.json'), true);
    }

    /**
     * @return array{
     *     foo: string,
     *     bar: int,
     *     subArr: array{
     *         subValue1: \DateTime,
     *         subValue2: array<int>,
     *         subValue3: array<string, int>,
     *         subValue4: \Lib\Option<\Lib\Collection<\App\Model\Customer>>,
     *     }
     * }
     */
    private function getPsalmComplexArray()
    {
        return json_decode(file_get_contents('asdsad.json'), true);
    }

    public function providePsalmArrayReturn()
    {
        $arr = $this->getPsalmArray();
        $arr[''];
        $complexArr = $this->getPsalmComplexArray();
        $complexArr[''];
        $complexArr['subArr'][''];
        return [
            [$arr, ['bar', 'foo']],
            [$complexArr, ['bar', 'foo', 'subArr']],
            [$complexArr['subArr'], ['subValue1', 'subValue2', 'subValue3', 'subValue4']],
        ];
    }

    /**
     * @param array{foo: string, bar: int} $arg
     * @param array<array{foo: string, bar: int}> $moreArgs
     */
    public function providePsalmArrayArg($arg, $moreArgs)
    {
        $arg[''];
        $moreArgs[0][''];
        return [
            [$arg, ['bar', 'foo']],
            [$moreArgs[0], ['bar', 'foo']],
        ];
    }

    public function providePsalmGenerator()
    {
        /** @var \Generator<array{
         *                      itemNo:string,
         *                      variants:array<array{
         *                          Code:string,
         *                          stock:array<array{
         *                                     serialNo:string,
         *                                     locationCode:string,
         *                                     differentialTaxation:bool
         *                                      }>
         *                           }>
         *                      }>
         * $products
         */
        $products = iterateOverProducts();
        $products[''];
        $first = null;
        foreach ($products as $product) {
            $first = $product;
            break;
        }
        $first['variants'][0]['stock'][0][''];
        return [
            [$first, ['itemNo', 'variants']],
            [$first['variants'][0], ['Code', 'stock']],
            [$first['variants'][0]['stock'][0], ['serialNo', 'locationCode', 'differentialTaxation']],
        ];
    }

    public function provideInterfaceReturnDoc()
    {
        $data = (new \MyClass())->getData();
        $data[''];
        return [
            [$data, ['key1', 'key2']],
        ];
    }

    /**
     * @template T
     * @psalm-param T $t
     * @return T
     */
    function deepCopy($t) {
        $serialized = var_export($t, true);
        return \SomeLiv\VarExportParser::parse($serialized);
    }

    public function providePsalmGeneric()
    {
        $reimu = new ReimuHakurei();
        $reimu->demandDonuts()[''];
        $copied = $this->deepCopy($reimu);
        $copied->demandDonuts()[''];
        return [
            [$copied->demandDonuts(), ['patience', 'amount', 'consequences']],
        ];
    }

    /**
     * @template T_EL
     * @template T_KEY as array-key
     * @param callable<T_EL, string> $getKey
     * @param array<T_EL> $arr
     * @return array<T_KEY, array<T_EL>>
     */
    function groupBy(callable $getKey, array $arr) {
        // should not use implementation for completion - jut the generics in the doc
    }

    public function providePsalmGenericArr()
    {
        $segments = [
            ['num' => 1, 'to' => 'RIX', 'date' => '2019-09-15'],
            ['num' => 2, 'to' => 'LON', 'date' => '2019-09-18'],
            ['num' => 3, 'to' => 'MOW', 'date' => '2019-09-21'],
        ];
        $getKey = function($seg) {return $seg['num'];};
        $grouped = $this->groupBy($getKey, $segments);
        $grouped[1][0][''];
        return [
            [$grouped[1][0], ['num', 'to', 'date']],
        ];
    }

    public function provideArrayColumnThirdArg()
    {
        $items = [
            ['type' => 'airport', 'value' => 'JFK'],
            ['type' => 'airport', 'value' => 'KBP'],
            ['type' => 'city', 'value' => 'NYC'],
            ['type' => 'city', 'value' => 'LON'],
        ];
        $codeToRow = array_column($items, null, 'value');
        $codeToRow[''];
        return [
            [$codeToRow, ['JFK', 'KBP', 'NYC', 'LON']],
            [$codeToRow['NYC'], ['type', 'value']],
        ];
    }

    public function provideCompletionFromUnrelatedClassOfSameIfc($arg)
    {
        $firstRow = (new Issue087\ClassFirstPhpDocInheritKeys\ExactKeysUnitTest)->formatOneRow($arg);
        $secondRow = (new Issue087\ClassSecondPhpDocInheritKeys\ExactKeysUnitTest)->formatOneRow($arg);
        // should not suggest 'performed', as it is only declared in
        // ClassSecondPhpDocInheritKeysUnitTest, not ClassFirstPhpDocInheritKeysUnitTest
        $firstRow[''];
        // should not suggest cmd_performed
        $secondRow[''];
        return [
            [$firstRow, ['id', 'cmd_performed']],
            [$secondRow, ['id', 'performed']],
        ];
    }

    /**
     * @param \Lib\ResultGen<\TouhouNs\ReimuHakurei> $result
     */
    public function providePsalmClsGeneric($result)
    {
        $fromField = $result->result;
        $fromField->d;
        $unwrapped = $result->unwrap();
        $unwrapped->d;
        return [
            [$fromField->demandDonuts(), ['patience', 'amount', 'consequences']],
            [$unwrapped->demandDonuts(), ['patience', 'amount', 'consequences']],
        ];
    }

    public function getCombinations()
    {
        while (rand() % 10 !== 0) {
            $pqId = ['old', 'fresh', 'purged'][rand()];

            $priceQuote = [
                'pqId' =>  123,
                'searchKey' => 234,
                'packageKey' => 456,
                'pricingOptionOrder' => 4564,
                'combinationOrder' => 586
            ];

            yield $pqId => $priceQuote;
        }
    }

    public function provideYieldWithKey()
    {
        $firstCombo = null;
        $result = [];
        foreach ($this->getCombinations() as $key => $combo) {
            $result[$key] = $combo;
        }
        return [
            [$result, ['old', 'fresh', 'purged']],
            [$result['old'], ['pqId', 'searchKey', 'packageKey', 'pricingOptionOrder', 'combinationOrder']],
        ];
    }

    /**
     * @param array{a:int}|array{b:int} $simple
     * @param array<
     *     array{user: array{id: 123, name: 'Joe'}} |
     *     array{order: array{id: 21, offer_id: 212}} |
     *     array{offer: array{id: 212, name: 'Toys'}}
     * > $records
     */
    public function providePsalmOr($simple, $records)
    {
        $simple[''];
        $records[''];
        $records[0]['order'][''];
        return [
            [$simple, ['a', 'b']],
            [$records[0], ['user', 'order', 'offer']],
            [$records[0]['user'], ['id', 'name']],
            [$records[0]['order'], ['id', 'offer_id']],
            [$records[0]['offer'], ['id', 'name']],
        ];
    }

    /**
     * @param $data['brand_detail'] = new ReimuHakurei
     * @param $data['price'] = ['currency' => 'USD', 'amount' => '200.00']
     */
    public function provideDocForSingleKey($data)
    {
        /**
         * @var $data['availability'] = ['free' => 18, 'reserved' => 3]
         * @var $data['provider']['contacts'] = ['email' => 'support@opel.com', 'phone' => '+37199456874']
         */
        $data[''];
        return [
            [$data, ['brand_detail', 'price', 'availability', 'provider']],
            [$data['price'], ['currency', 'amount']],
            [$data['availability'], ['free', 'reserved']],
            [$data['provider'], ['contacts']],
            [$data['provider']['contacts'], ['email', 'phone']],
        ];
    }

    public function provideCtorGenerics()
    {
        $fromCtor = new ResultGen(true, new KiraYoshikage());
        $fromCtor->unwrap()->e;
        return [
            [$fromCtor->unwrap()->bitesZaDusto(), ['time', 'goes', 'back']],
        ];
    }

    public function provideMorePsalmGenerics()
    {
        $fromStaticMeth = ResultGen::makeOk(new KiraYoshikage());
        $fromStaticMeth->unwrap()->bitesZaDusto()[''];
        $mapped = $fromStaticMeth->map(function($reimu){return new MarisaKirisame();});
        ([$mapped][0])->unwrap()->m;
        return [
            [$fromStaticMeth->unwrap()->bitesZaDusto(), ['time', 'goes', 'back']],
            [$mapped->unwrap()->masterSpark(), ['deadFoes']],
        ];
    }

    //=============================
    // following are not implemented yet
    //=============================

    //===============================
    // TODO: testify following
    //===============================

    private static function testIndexedArrayCreation()
    {
        $records = [
            ['result' => -100],
            ['error' => 'lox'],
        ];
        // should suggest result
        $records[0][''];
        // should suggest error
        $records[1][''];
        return [
            [$records[0], ['result']],
            [$records[1], ['error']],
        ];
    }

    private static function testClosureInference()
    {
        $func = function($i) {
            return [
                'asdad' => 'asda',
                'qweq' => $i * 2,
            ];
        };
        $record = $func(123);
        // should suggest asdad, qweq
        $record[''];
        return [
            [$record, ['asdad', 'qweq']],
        ];
    }

    private static function testTupleAccess()
    {
        $recordA = ['aField1' => 13, 'aField2' => 234.42];
        $recordB = ['bField1' => [1,2,3], 'bField2' => 'asdasdd'];
        $tuple = [$recordA, $recordB];
        // should suggest aField1, aField2
        $tuple[0][''];
        // should suggest bField1, bField2
        $tuple[1][''];
        list($restoredA, $restoredB) = $tuple;
        // should suggest aField1, aField2
        $restoredA[''];
        // should suggest bField1, bField2
        $restoredB[''];
        return [
            [$tuple[0], ['aField1', 'aField2']],
            [$tuple[1], ['bField1', 'bField2']],
            [$restoredA, ['aField1', 'aFie;d2']],
            [$restoredB, ['bField1', 'bFie;d2']],
        ];
    }

    private static function testLambdaAccess()
    {
        $mapped = array_map(function($subject) {
            return [
                // should suggest name, priority
                'name2' => $subject[''].'_2',
                'priority2' => $subject[''] + 4,
            ];
        }, $subjects);
        // should suggest name2, priority2
        $mapped[2][''];
        return [
            [$mapped[2], ['name2', 'priority']],
        ];
    }

    private static function testBasisListAccess()
    {
        $makeTax = function($i) {
            return [
                'currency' => -'USD',
                'amount' => 199 + $i,
            ];
        };
        return \array_map($makeTax, [1,2,3]);
    }

    private static function testListAccess()
    {
        $mapped = self::testBasisListAccess();
        $addTaxCode = function(array $taxRecord) {
            $taxRecord['taxCode'] = 'YQ';
            return $taxRecord;
        };
        $withTaxCode = array_map($addTaxCode, $mapped);
        // should suggest currency, amount, taxCode
        $withTaxCode[0][''];
        return [
            [$withTaxCode[0], ['currency', 'amount', 'taxCode']],
        ];
    }
}

/**
 * @method getById(int $id) = [
 *     'id' => 1,
 *     'username' => 'user_name'
 * ]
 * @method getAll() = [
 *     0 => self::getById(1)
 * ]
 * @method jsonSerialize() = [
 *     'nameRecord' => $this->nameRecord,
 *     'posts' => ['today is sunny', 'walking my dog', 'eating an icecream'],
 * ]
 * @property $paxTypes = [$this->normalizePassengerType()]
 */
class users_table {
    private $nameRecord = [
        'lastName' => 'Pupkin',
        'firstName' => 'Vasya',
        'number' => '4',
    ];

    private $base = [
        'age' => 19,
        'ptc' => 'JCB',
        'fareGroup' => 'inclusiveBulk',
    ];

    /** @param $base = $this->base */
    public function normalizePassengerType($base)
    {
        $base[''];
        $base['ageGroup'] = $base['ptc'] === 'CNN' ? 'child' : 'adult';
        return $base;
    }
}

class FooBar
{
    const KEY_1 = ['a' => 123];
    const KEY_2 = ['b' => 123];
    const KEY_3 = ['c' => 123];
    public static $prop = ['p' => 123];

    public function getFoo(): string
    {
        $cls = self::class;
        return $cls::$KEY_1;
    }
}
