<?php
namespace DeepTest;

/**
 * calls UsageResolver on the argument of each provide* function and
 * tests that it's return value matches the inferred argument type
 */
class UsageResolverUnitTest
{
    public function provideUsedKeys($params)
    {
        $soapData = [
            'PNR_ID' => $params['recordLocator'],
            'LAST_NAME' => $params['lastName'],
            'FIRST_NAME' => $params['firstName'],
        ];
        book_reservation($soapData);

        return [
            'params' => [
                'recordLocator' => [],
                'lastName' => [],
                'firstName' => [],
            ],
        ];
    }

    /**
     * @param $params = [
     *     'gds' => ['apollo', 'sabre', 'galileo', 'amadeus'][$any],
     *     'pcc' => '2CV5' ?: 'L3IY' ?: 'SFO123123',
     *     'passengers' => [
     *         ['lastName' => 'Pupkin', 'firstName' => 'Vasya', 'age' => 19],
     *         ['lastName' => 'Pupkin', 'firstName' => 'Larisa', 'age' => 23],
     *     ],
     * ]
     */
    private static function createPnr($params)
    {
    }

    public function provideUsageFromDoc($params)
    {
        self::createPnr($params);

        return [
            'params' => [
                'gds' => ['apollo', 'sabre', 'galileo', 'amadeus'][rand()],
                'pcc' => '2CV5' ?: 'L3IY' ?: 'SFO123123',
                'passengers' => [
                    ['lastName' => 'Pupkin', 'firstName' => 'Vasya', 'age' => 19],
                    ['lastName' => 'Pupkin', 'firstName' => 'Larisa', 'age' => 23],
                ],
            ],
        ];
    }

    private static function makeAddSsrCmd($data)
    {
        $cmd = '@:3DOCS'.implode('/', [
            $data['docType'],
            $data['docCountry'],
            $data['docNumber'],
            $data['gender'],
            $data['dob'],
            $data['lastName'],
            $data['firstName'],
        ]);
        return $cmd;
    }

    /**
     * @param $pq = [
     *     'gds' => 'apollo',
     *     'pcc' => 'LFS5',
     *     'pnrDump' => 'ASD123/WS LAXFS ...',
     *     'pricingDump' => '>$BN1|2*C05 ...',
     * ]
     */
    private static function sendPqToGoogle($pq)
    {
        file_get_contents('http://google.com/accept-pq?'.json_encode($pq));
    }

    private static function provideReverseType($ssrParams, $pqParams)
    {
        self::makeAddSsrCmd($ssrParams);
        self::makeAddSsrCmd([
            // should suggest: "docType", "docCountry", "docNumber", "gender", "dob", "lastName", "firstName"
            '' => 123,
            // should suggest same
            //'' => ,
            // should suggest same
            ''
        ]);
        self::makeAddSsrCmd([
            'docType' => 'PASS',
            'docCountry' => 'US',
            // should suggest all except "docType" and "docCountry"
            'docNumber' => '',
            '' => '',
        ]);
        self::sendPqToGoogle($pqParams);
        self::sendPqToGoogle([
            // should suggest: "gds", "pcc", "pnrDump", "pricingDump"
            '' => 'sabre',
        ]);
        return [
            'ssrParams' => [
                'docType' => [],
                'docCountry' => [],
                'docNumber' => [],
                'gender' => [],
                'dob' => [],
                'lastName' => [],
                'firstName' => [],
            ],
            'pqParams' => [
                'gds' => 'apollo',
                'pcc' => 'LFS5',
                'pnrDump' => 'ASD123/WS LAXFS ...',
                'pricingDump' => '>$BN1|2*C05 ...',
            ],
        ];
    }

    private static function testUsedKeysInAVar()
    {
        $params = [
            // should suggest: "docType", "docCountry", "docNumber", "gender", "dob", "lastName", "firstName"
            '' => 123,
        ];
        $cmd = self::makeAddSsrCmd($params);
    }

    public function provideConstructorCompletion($params)
    {
        new \TouhouNs\MarisaKirisame($params);
        $marisa = new \TouhouNs\MarisaKirisame([
            // should suggest: "ability", "bombsLeft", "livesLeft", "power"
            '' => 'Master Spark',
        ]);
        return [
            'params' => [
                'ability' => [],
                'bombsLeft' => [],
                'livesLeft' => [],
                'power' => [],
            ],
        ];
    }

    private static function makeDefaultApolloState()
    {
        return [
            'gds' => 'apollo',
            'area' => 'A',
            'pcc' =>  '2G55',
            'record_locator' => null,
            'has_pnr' => false,
            'is_pnr_stored' => false,
            'can_create_pq' => false,
            'id' => 1,
            'internal_token' => 'fake123',
            'agent_id' => 6206,
            'lead_creator_id' => 6206,
            'lead_id' => 1,
        ];
    }

    private static function provideReplaceCompletion($mergeArg, $replaceArg)
    {
        $testState = array_merge(self::makeDefaultApolloState(), $mergeArg);
        $testState = array_merge(self::makeDefaultApolloState(), [
            '' => true, // should suggest: gds, area, pcc, etc...
        ]);
        $destState = array_replace(self::makeDefaultApolloState(), $replaceArg);
        $destState = array_replace(self::makeDefaultApolloState(), [
            '' => true, // should suggest: gds, area, pcc, etc...
        ]);
        return [
            'mergeArg' => [
                'gds' => 'apollo',
                'area' => 'A',
                'pcc' =>  '2G55',
                'record_locator' => null,
                'has_pnr' => false,
                'is_pnr_stored' => false,
                'can_create_pq' => false,
                'id' => 1,
                'internal_token' => 'fake123',
                'agent_id' => 6206,
                'lead_creator_id' => 6206,
                'lead_id' => 1,
            ],
            'replaceArg' => [
                'gds' => 'apollo',
                'area' => 'A',
                'pcc' =>  '2G55',
                'record_locator' => null,
                'has_pnr' => false,
                'is_pnr_stored' => false,
                'can_create_pq' => false,
                'id' => 1,
                'internal_token' => 'fake123',
                'agent_id' => 6206,
                'lead_creator_id' => 6206,
                'lead_id' => 1,
            ],
        ];
    }

    private static function providePlusCompletion($params)
    {
        $testState = self::makeDefaultApolloState() + $params;
        $testState = self::makeDefaultApolloState() + [
            '' => true, // should suggest: gds, area, pcc, etc...
        ];
        return [
            'params' => [
                'gds' => 'apollo',
                'area' => 'A',
                'pcc' =>  '2G55',
                'record_locator' => null,
                'has_pnr' => false,
                'is_pnr_stored' => false,
                'can_create_pq' => false,
                'id' => 1,
                'internal_token' => 'fake123',
                'agent_id' => 6206,
                'lead_creator_id' => 6206,
                'lead_id' => 1,
            ],
        ];
    }

    private static function transformAvailabilityParams($params)
    {
        return [
            'Id' => $params['id'],
            'From' => $params['from'],
            'To' => $params['to'],
        ];
    }

    private static function getAvailability($params)
    {
        $soapParams = self::transformAvailabilityParams($params);
        return callSoap($soapParams);
    }

    private static function provideDeepUsageCompletion($params)
    {
        $availability = self::getAvailability($params);
        $availability = self::getAvailability([
            // should suggest: id, from, to
            '' => 123,
        ]);
        return [
            'params' => [
                'id' => [],
                'from' => [],
                'to' => [],
            ],
        ];
    }

    public function provideAbstractMethodUsedKeys($params)
    {
        $result = (new \Gtl\AmadeusSoapActions\AmadeusGetFareRulesAction('GENERIC'))->execute($params);
        $result = (new \Gtl\AmadeusSoapActions\AmadeusGetFareRulesAction('GENERIC'))
            ->execute([
                ''
            ]);
        return [
            'params' => [
                'origin' => [],
                'destination' => [],
                'ticketingDt' => [],
                'airline' => [],
                'departureDt' => [],
                'fareBasis' => [],
                'ticketDesignator' => [],
            ],
        ];
    }

    private static function makeCoolOutfit($materials)
    {
        return [
            'hat' => $materials['cardboard'] + $materials['ebony'],
            'jacket' => $materials['wool'] + $materials['iron'],
            'boots' => $materials['wood'] + $materials['linenCloth'] + $materials['leather'],
        ];
    }

    private static function makeHero($params)
    {
        return [
            'story' => 'Once upon a time there was a here called '.$params['name'].'. '.
                'After some struggle, he defeated the '.$params['enemyName'].' and saved the world. '.
                'He then married '.$params['nameOfTheLovedOne'].' and lived a happy live.',
            'outfit' => self::makeCoolOutfit($params['outfitMaterials']),
        ];
    }

    private static function spawnUnderling($params)
    {
        return [
            'characterValue' => rand(0,100),
            'character' => self::makeHero($params),
        ];
    }

    private static function provideUsedKeysPassedDeeper($heroParams, $underParams, $validationParams)
    {
        $hero = self::makeHero($heroParams);
        $hero = self::makeHero([
            // should suggest: "name", "enemyName", etc...
            '' => 'Bob',
            'outfitMaterials' => [
                // should suggest: "wood", "wool", "iron", etc...
                '' => '',
            ],
            'underling' => self::spawnUnderling($underParams),
            'underling' => self::spawnUnderling([
                // should suggest: "name", "enemyName", etc...
                '' => 'Jim',
            ]),
        ]);
        \DeepTest\ExactKeysUnitTest::provideParamValidation($validationParams);
        \DeepTest\ExactKeysUnitTest::provideParamValidation([
            'itinerary' => [
                [
                    'lastName' => '',
                ],
            ],
            'commission' => [
                '' => '',
            ],
        ]);
        return [
            'heroParams' => [
                'name' => [],
                'enemyName' => [],
                'nameOfTheLovedOne' => [],
                'outfitMaterials' => [
                    'cardboard' => [],
                    'ebony' => [],
                    'wool' => [],
                    'iron' => [],
                    'wood' => [],
                    'linenCloth' => [],
                    'leather' => [],
                ],
            ],
            'underParams' => [
                'name' => [],
                'enemyName' => [],
                'nameOfTheLovedOne' => [],
                'outfitMaterials' => [
                    'cardboard' => [],
                    'ebony' => [],
                    'wool' => [],
                    'iron' => [],
                    'wood' => [],
                    'linenCloth' => [],
                    'leather' => [],
                ],
            ],
            'validationParams' => [
                'recordLocator' => [
                    'units' => 'sample',
                    'value' => 'sample',
                ],
                'passengers' => [
                    'units' => 'sample',
                    'value' => 'sample',
                ],
                'itinerary' => [
                    'units' => 'sample',
                    'value' => 'sample',
                ],
                'commission' => [
                    'units' => 'sample',
                    'value' => 'sample',
                ],
                null => 'sample',
            ] ?: 'sample',
        ];
    }

    private static function bookHotelSegments($params)
    {
        $remarks = [];
        $remarks[] = $params['remarks'][0]['text'];
        $remarks[] = $params['remarks'][1]['text'];
        $query = http_build_query([
            'last_name' => $params['lastName'],
            'first_name' => $params['firstName'],
            'remarks' => $remarks,
            'segments' => array_map(function($seg){return [
                'date' => $seg['date'],
                'fare_basis' => $seg['fareBasis'],
                'vendor' => $seg['vendor'],
                'property_code' => $seg['propertyCode'],
            ];}, $params['segments']),
        ]);
        return file_get_contents('http://midiana.lv/?'.$query);
    }

    private static function provideUsedKeysArray($params)
    {
        static::bookHotelSegments($params);
        static::bookHotelSegments([
            'remarks' => [
                [
                    // should suggest: text... one day
                    '' => 'DEV TESTING PLS IGNORE',
                ],
            ],
            'segments' => [
                [
                    // should suggest: date, fareBasis, vendor, propertyCode
                    '' => '2018-12-10',
                ],
                '1' => [
                    // should suggest: date, fareBasis, vendor, propertyCode
                    '' => '2018-12-10',
                ],
            ],
        ]);
        return [
            'params' => [
                'remarks' => [],
                'lastName' => [],
                'firstName' => [],
                'segments' => [
                    null => [
                        'date' => [],
                        'fareBasis' => [],
                        'propertyCode' => [],
                        'vendor' => [],
                    ],
                ],
            ],
        ];
    }

    private static function providePdoUsedColonParams($params)
    {
        $prepared = (new \PDO("blablabla"))
            ->prepare('SELECT * FROM delete_me WHERE name = :name AND price < :price;');
        $prepared->execute($params);
        $prepared->execute([
            // should suggest: name, price
            '' => '123',
        ]);
        return [
            'params' => [
                'name' => [],
                'price' => [],
            ],
        ];
    }

    /**
     * @param $field = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][-100]
     */
    private function weekday(string $field)
    {
        print('Pysch! Processed '.$field.PHP_EOL);
    }

    private function provideStringArgCompletion($param)
    {
        // should suggest: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
        $this->weekday('');
        $this->weekday($param);
        return [
            'param' => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][rand()],
        ];
    }

    private function getProfile(string $profileName)
    {
        $profiles = [
            'dev' => ['password' => 'qwerty123'],
            'prod' => ['password' => 'qwerty456'],
            'student' => ['password' => 'qwerty789'],
        ];
        return $profiles[$profileName];
    }

    private function provideStringArrayKeyArgCompletion($param)
    {
        // should suggest: ['dev', 'prod', 'student']
        $this->getProfile('');
        $this->getProfile($param);
        return [
            'param' => ['dev', 'prod', 'student'][rand()],
        ];
    }

    public function provideSetDynamicPropsFromArr($params)
    {
        $nepgear = \DeepTest\Nepgear::__set_state($params);
        $nepgear = \DeepTest\Nepgear::__set_state([
            // should suggest: weapon, bracer, pants, armor
            '' => 'Cursed Sword',
        ]);
        return [
            'params' => [
                'weapon' => [],
                'bracer' => [],
                'pants' => [],
                'armor' => [],
            ],
        ];
    }

    public function provide_stream_context_create($params)
    {
        stream_context_create($params);
        stream_context_create([
            'http' => [
                ''
            ],
            'ftp' => [
                '' => '',
            ],
        //    'phar' => [
        //        ''
        //    ],
        ]);
        return [
            'params' => [
                "http" => [
                    "header" => "Content-type: application/x-www-form-urlencoded\r\n",
                    "method" => ["GET", "POST", "OPTIONS", "PUT", "HEAD", "DELETE", "CONNECT", "TRACE", "PATCH"][rand()],
                    "content" => "name=Vasya&age=26&price=400",
                    "user_agent" => "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/71.0.3578.80 Chrome/71.0.3578.80 Safari/537.36",
                    "proxy" => "tcp://proxy.example.com:5100",
                    "request_fulluri" => [],
                    "follow_location" => [],
                    "max_redirects" => [],
                    "protocol_version" => [],
                    "timeout" => [],
                    "ignore_errors" => [],
                ],
                "socket" => [
                    "bindto" => "128.211.185.166:3345",
                    "backlog" => [],
                    "ipv6_v6only" => [],
                    "so_reuseport" => [],
                    "so_broadcast" => [],
                    "tcp_nodelay" => [],
                ],
                "ftp" => [
                    "overwrite" => [],
                    "resume_pos" => [],
                    "proxy" => "tcp://squid.example.com:8000",
                ],
                "ssl" => [
                    "peer_name" => [],
                    "verify_peer" => [],
                    "verify_peer_name" => [],
                    "allow_self_signed" => [],
                    "cafile" => "/path/to/cert/auth/file",
                    "capath" => "/path/to/cert/auth/dir",
                    "local_cert" => "/path/to/cert.pem",
                    "local_pk" => "/path/to/private/key.pem",
                    "passphrase" => "qwerty123",
                    "verify_depth" => [],
                    "ciphers" => "ALL:!COMPLEMENTOFDEFAULT:!eNULL",
                    "capture_peer_cert" => [],
                    "capture_peer_cert_chain" => [],
                    "SNI_enabled" => [],
                    "disable_compression" => [],
                    "peer_fingerprint" => "tcp://squid.example.com:8000",
                ],
                "phar" => [
                    "compress" => [],
                    "metadata" => [],
                ],
                "zip" => [
                    "cafile" => "qwerty123"
                ],
            ],
        ];
    }

    function provide_image_type_to_mime_type($param)
    {
        image_type_to_mime_type($param);
        image_type_to_mime_type();
        return [
            'param' => [
                IMAGETYPE_GIF     , // "image/gif"
                IMAGETYPE_JPEG    , // "image/jpeg"
                IMAGETYPE_PNG     , // "image/png"
                IMAGETYPE_SWF     , // "application/x-shockwave-flash"
                IMAGETYPE_PSD     , // "image/psd"
                IMAGETYPE_BMP     , // "image/bmp"
                IMAGETYPE_TIFF_II , // "(intel byte order)	image/tiff"
                IMAGETYPE_TIFF_MM , // "(motorola byte order)	image/tiff"
                IMAGETYPE_JPC     , // "application/octet-stream"
                IMAGETYPE_JP2     , // "image/jp2"
                IMAGETYPE_JPX     , // "application/octet-stream"
                IMAGETYPE_JB2     , // "application/octet-stream"
                IMAGETYPE_SWC     , // "application/x-shockwave-flash"
                IMAGETYPE_IFF     , // "image/iff"
                IMAGETYPE_WBMP    , // "image/vnd.wap.wbmp"
                IMAGETYPE_XBM     , // "image/xbm"
                IMAGETYPE_ICO     , // "image/vnd.microsoft.icon"
                IMAGETYPE_WEBP    , // "image/webp
            ][rand()],
        ];
    }

    function provide_imageaffine($affine, $clip)
    {
        imageaffine($img, $affine, $clip);
        imageaffine($img, [
            // no completion because I disabled numeric key completion
            // because many ambiguous types with trashy completion options
            '' => '',
        ], [
            '' => '',
        ]);
        return [
            'affine' => [
                0 => [],
                1 => [],
                2 => [],
                3 => [],
                4 => [],
                5 => [],
            ],
            'clip' => [
                'x' => [],
                'y' => [],
                'width' => [],
                'height' => [],
            ],
        ];
    }

    /**
     *
     * @param string $directory
     *
     * @param array  $options  = [
     *                         "sort_by" => ["name","type","modified_time","accessed_tim","changed_type"][$i]
     *                         ]
     * @return array
     */
    public static function directories($directory = "", $options = [])
    {
        if ($options['sort_by'] === '') {

        }
    }

    public static function provideKeyStrValCompletion($params)
    {
        self::directories('/var/www', $params);
        self::directories('/var/www', ['sort_by' => 'accessed_tim']);
        return [
            'params' => [
                'sort_by' => ["name","type","modified_time","accessed_tim","changed_type"][rand()],
            ],
        ];
    }

    //============================
    // TODO: testify following
    //============================

    public function testBuiltInArgTypeDefs()
    {
        proc_open('ls', [0 => 1], &$pipes, $cwd, $env, [
            ''
        ]);

        str_pad('asd', '0', '');

        json_encode(null, '');

        pcntl_signal(SIGQUIT, function($code, $info){
            $code === SIGILL;
            $info['code'];
        });

        $curl = curl_init('http://google.com');
        curl_setopt($curl, '', 1234);
        curl_setopt_array($curl, [
            CURLOPT_FTP_USE_EPSV => '',
        ]);
        pcntl_signal();
        file_put_contents('text.txt', 'abababa', );
        json_encode(['a' => 5, 'b' => 6], JSON_PRETTY_PRINT | );
        file_put_contents('text.txt', 'abababa', '');

        preg_match('/asd/', 'asd', $matches, '');
        preg_split('/asd/', 'asd', -1, '');
        preg_match_all('/asd/', 'asd', $matches, '');
        simplexml_load_string('<root/>', null, '2.9.1');
        simplexml_load_file('<root/>', null, '0');
        preg_last_error() === '';
    }

    private static function testPdoUsedColonParamsThruLib()
    {
        \Lib\Db::inst()->exec(implode(PHP_EOL, [
            'SELECT * FROM delete_me WHERE name = :name AND price < :price;',
        ]), [
            // TODO: does not work because multiple declarations, should fix!
            // should suggest: name, price
            '' => '123',
        ]);
    }

    private static function testLaravelModelCtorParams()
    {
        // TODO: deos not work anymore, should fix!
        $competitor = new \App\Orm\Competitor([
            // should suggest: id, created_at, updated_at, spice_left
            '' => 123,
        ]);
    }
}
