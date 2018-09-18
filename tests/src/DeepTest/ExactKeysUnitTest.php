<?php
namespace DeepTest;

use Lib\ParamValidation\DictP;
use Lib\ParamValidation\ListP;
use Lib\ParamValidation\StringP;

/**
 * unlike UnitTest.php, this test not just checks that actual result  has _at least_
 * such keys, but it tests that it has _exactly_ such keys, without extras
 */
class ExactKeysUnitTest
{
    public function provideAssocBuiltIns($handle)
    {
        $handle = curl_init('google.com');
        $response = curl_exec($handle);
        $curlInfo = curl_getinfo($handle);
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
        $list[] = [$procMeta, [
            'command', // string - The command string that was passed to proc_open().
            'pid', // int - process id
            'running', // bool - TRUE if the process is still running, FALSE if it has terminated.
            'signaled', // bool - TRUE if the child process has been terminated by an uncaught signal. Always set to FALSE on Windows.
            'stopped', // bool - TRUE if the child process has been stopped by a signal. Always set to FALSE on Windows.
            'exitcode', // int - The exit code returned by the process (which is only meaningful if running is FALSE). Only first call of this function return real value, next calls return -1.
            'termsig', // int - The number of the signal that caused the child process to terminate its execution (only meaningful if signaled is TRUE).
            'stopsig', // int - The number of the signal that caused the child process to stop its execution (only meaningful if stopped is TRUE).
        ]];
        $rusage = getrusage();
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
        $list[] = [$lastError, [
            'type', // 2,
            'message', // 'proc_get_status() expects parameter 1 to be resource, null given',
            'file', // 'php shell code',
            'line', // 1,
        ]];
        $dnsRecord = dns_get_record('google.com');
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
        $list[] = [$stat, [
            'dev', // 2049,
            'ino', // 13238274,
            'mode', // 16877,
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
        return $list;
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
            'teamInitials' =>'  ',
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
        return json_decode(file_get_contents('/var/www/html/local_config.json'), true);
    }

    public function provideReturnDoc()
    {
        $config = static::getLocalConfig();
        $config['fluentdPort'];
        $list[] = [$config, ['baseUrl', 'isDevelopment', 'fluentdIp', 'fluentdPort']];
        $config2 = static::getGlobalConfig();
        $config2[''];
        $list[] = [$config2, ['securityKey', 'loggerIp']];
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
}
