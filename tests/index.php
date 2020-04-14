<?php

use DeepTest\TestData;
use Illuminate\Database\Eloquent\Model, Lib\Utils\Fp;
use NeptuniaNs\Plutia;

class DeepKeysTest
{
    private static function makeRecord()
    {
        return TestData::makeStudentRecord();
    }

    function makeRecordMaybe()
    {
        return rand() < 0.5 ? self::makeRecord() : null;
    }

    //-------------------------------
    // string value completion start (would be cool to move all that to UsageResolver)
    //-------------------------------

    private static function testArrayColumn()
    {
        $segments = [
            ['from' => 'MOW', 'to' => 'LON'],
            ['from' => 'LON', 'to' => 'PAR'],
            ['from' => 'PAR', 'to' => 'MOW'],
        ];
        // should suggest: "from", "to"
        $segments[0][''];
        // should suggest: "from", "to"
        array_column($segments, '');

        $pnrs = [];
        $pnrs[] = [
            'ptcInfo' => ['ptc' => 'C05', 'quantity' => 1],
            'paxes' => [['name' => 'vova', 'surname' => 'turman']],
            'itinerary' => $segments,
        ];
        array_column($pnrs, '');
        array_column(array_column($pnrs, 'ptcInfo'), '');
    }

    private static function testArrayKeyExists()
    {
        $segments = [
            ['from' => 'MOW', 'to' => 'LON'],
            ['from' => 'LON', 'to' => 'PAR'],
            ['from' => 'PAR', 'to' => 'MOW'],
        ];
        // should suggest: "from", "to"
        $segments[0][''];
        // should suggest: "from", "to"
        array_key_exists('from', $segments[0]);

        $pnrs = [];
        $pnrs[] = [
            'ptcInfo' => ['ptc' => 'C05', 'quantity' => 1],
            'paxes' => [['name' => 'vova', 'surname' => 'turman']],
            'itinerary' => $segments,
        ];
        array_key_exists('itinerary', $pnrs[0]);
    }

    /**
     * @param $asd = PersonStorage::addPerson()['']
     */
    private static function testInArray($asd)
    {
        $i = rand(0, 3);
        $types = ['AIR', 'CAR', 'HOTEL', 'RAIL'];
        $type = $types[$i];
        if ($type === '') {

        }
        if (in_array($type, ['CAR', ''])) {

        }
        if (in_array('', $types)) {

        }
        if (array_intersect([''], $types, ['ASD'])) {

        }
        if (($types[$i] ?? null) === '') {

        }
    }

    private static function testEqualsStringValues()
    {
        if (rand() % 1) {
            $type = 'DOCO';
        } elseif (rand() % 1) {
            $type = 'DOCA';
        } elseif (rand() % 1) {
            $type = 'DOCS';
        } elseif (rand() % 1) {
            $type = 'FQTV';
        }
        // should suggest: DOCO, DOCA, DOCS, FQTV
        if ($type === 'DOCA') {

        }
        // should also suggest: DOCO, DOCA, DOCS, FQTV
        if ($type !== '') {

        }
        $arr = ['asd' => 'lol'];
        $arr['asd'] === '';
    }

    //-------------------------------
    // string value completion end
    //-------------------------------

    private static function testArrMethRef(KiraYoshikage $argObj)
    {
        // should suggest: "bombTransmutation", "sheerHeartAttack", "bitesZaDusto"
        array_map([\DeepTest\KiraYoshikage::class, ''], [1,2,3]);

        $kira = new \DeepTest\KiraYoshikage();
        // should suggest: "murder"
        $murderedNumbers = array_map([$kira, ''], [1,2,3]);
        $kiraFromMake = \DeepTest\KiraYoshikage::make();
        // should suggest: "murder", "getBadassness"
        $murderedNumbers = array_map([$kiraFromMake, ''], [1,2,3]);
        $murderedNumbers = array_map([$argObj, ''], [1,2,3]);
    }

    //----------------------------
    // anonymous function arg completion start (would be nice to cover by tests as well somehow, like looking for $arr[''] occurrences in the func)
    //----------------------------

    public function testLambdaAccess()
    {
        $subjects = self::makeRecord()['chosenSubSubjects'];
        $filtered = array_filter($subjects, function($subject) {
            // should suggest name, priority
            return $subject[''] > 4.0;
        });
        // should suggest name, priority
        $filtered[3][''];
    }

    private static function testGenericAccess()
    {
        $records = [];
        $records[] = [
            'key1' => 15,
            'key2' => 15,
            'subAssArray' => [
                'nestedKey1' => 12,
                'nestedKey2' => 12,
            ],
        ];

        $records[] = ['optionalKey' => 4];

        $mapped = array_map(function($record) {
            // should suggest key1, key2, subAssArray, optionalKey
            $record[''];
            return null;
        }, $records);
        $mapped = Fp::map(function($record) {
            // should suggest key1, key2, subAssArray, optionalKey
            $record[''];
            return null;
        }, $records);
    }

    private static function isOld($pcc)
    {
        return $pcc[''] < '1970';
    }

    public function provideFuncVarUsageBasedCompletion()
    {
        $list = [];
        $pccRecords = [
            ['gds' => 'apollo', 'pcc' => '1O4K', 'year' => '1971'],
            ['gds' => 'sabre', 'pcc' => '611F', 'year' => '1960'],
            ['gds' => 'amadeus', 'pcc' => 'RIX123456', 'year' => '1987'],
        ];
        $oldPccs = array_filter($pccRecords, [static::class, 'isOld']);

        $getPcc = function($pccRecord) use (&$list){
            // should suggest 'pcc' and 'gds' based on what
            // is passed to this lambda further in array_map
            $pccRecord[''];
            $list[] = [$pccRecord, ['pcc' => [], 'gds' => []]];
            return $pccRecord['pcc'];
        };
        $pccs = array_filter($pccRecords, $getPcc);

        return $list;
    }

    public function provideFuncVarUsageBasedCompletionDirectCall()
    {
        $list = [];

        $getPcc = function($pccRecord) use (&$list){
            // should suggest 'pcc' and 'gds' based on what
            // is passed to this lambda further in Fp::map
            $pccRecord[''];
            $list[] = [$pccRecord, ['pcc' => [], 'gds' => []]];
            return $pccRecord['pcc'];
        };
        $result = $getPcc(['gds' => 'apollo', 'pcc' => '1O4K']);

        return $list;
    }

    public function provideFuncVarUsageBasedCompletionFp()
    {
        $list = [];

        $pccRecords = [
            ['gds' => 'apollo', 'pcc' => '1O4K'],
            ['gds' => 'sabre', 'pcc' => '611F'],
            ['gds' => 'amadeus', 'pcc' => 'RIX123456'],
        ];
        $getPcc = function($pccRecord, $b) use (&$list){
            // should suggest 'pcc' and 'gds' based on what
            // is passed to this lambda further in Fp::map
            $pccRecord[''];
            $b['']; // should not suggest anything
            $list[] = [$pccRecord, ['pcc' => [], 'gds' => []]];
            return $pccRecord['pcc'];
        };
        $pccs = Fp::map($getPcc, $pccRecords);

        return $list;
    }

    public function provideFuncVarUsageBasedCompletionInline()
    {
        $list = [];

        $pccRecords = [
            ['gds' => 'apollo', 'pcc' => '1O4K'],
            ['gds' => 'sabre', 'pcc' => '611F'],
            ['gds' => 'amadeus', 'pcc' => 'RIX123456'],
        ];
        $pccs = Fp::map(function($pccRecord, $b) use (&$list){
            // should suggest 'pcc' and 'gds' based on what
            // is passed to this lambda further in Fp::map
            $pccRecord[''];
            $b['']; // should not suggest anything
            $list[] = [$pccRecord, ['pcc' => [], 'gds' => []]];
            return $pccRecord['pcc'];
        }, $pccRecords);

        return $list;
    }

    public function provideFuncVarUsageBasedCompletionMultiArg()
    {
        $list = [];

        $pccRecords = [
            ['gds' => 'apollo', 'pcc' => '1O4K'],
            ['gds' => 'sabre', 'pcc' => '611F'],
            ['gds' => 'amadeus', 'pcc' => 'RIX123456'],
        ];
        $getPcc = function($a, $b, $c) use (&$list){
            // should suggest 'pcc' and 'gds' based on what
            // is passed to this lambda further in usort
            $a[''];
            $b[''];
            $c['']; // should *not* suggest anything
        };
        usort($pccRecords, $getPcc);

        return $list;
    }

    public function provideFuncVarDirectCallPeekOutside()
    {
        $list = [];

        $mapper = function($id, $phone){
            // should suggest: model, price, ram, cores
            $phone[''];
        };
        $mapper(123, [
            'model' => 'Siemens A35',
            'price' => '123.50',
            'ram' => '512 MiB',
            'cores' => 2,
        ]);
    }

    private static function openFile(string $fileName)
    {
        return [
            'process' => 'OPEN_FILE',
            'resourceId' => rand(0,1000),
            'bytes' => [112,535,756,23423,3463],
            'createdDt' => '2018-01-13',
            'modifiedDt' => '2018-01-18',
        ];
    }

    private static function processFile(string $fileName, callable $processor)
    {
        $file = static::openFile($fileName);
        $processor($file);
        closeFile($fileName);
        $processor([
            'process' => 'CLOSE_FILE',
            'fileName' => $fileName,
        ]);
    }

    private static function provideLambdaArgCompletion()
    {
        $list = [];
        $processor = function($file) use (&$list){
            $file[''];
            $list[] = [$file, ['resourceId' => [], 'bytes' => [], 'createdDt' => [], 'modifiedDt' => []]];
        };
        static::processFile('anime_list.dat', $processor);
        return $list;
    }

    private static function testFpAnyAllPeekOutside()
    {
        $isGood = function($val){
            return $val[''] > 9;
        };
        $isCheep = function($val){
            return $val[''] < 2.00;
        };
        $books = [
            ['score' => 5, 'price' => 1.50],
            ['score' => 9.8, 'price' => 19.50],
            ['score' => 7.5, 'price' => 7.50],
        ];
        $gotGoodBooks = Fp::any($isGood, $books);
        $allBooksAreCheap = Fp::all($isCheep, $books);
    }

    //----------------------------
    // anonymous function arg completion end
    //----------------------------

    private static function testGoToIntKey()
    {
        $pair = [
            ['from' => 'KIV', 'to' => 'NYC'],
            ['name' => 'Vasya', 'age' => 24],
        ];
        $pair['0']; // _Ctrl + B_ should work
        $pair[0]; // _Ctrl + B_ should still work

        $values = [];
        $values['asdsad'] = 1;
        $values['qweqwe'] = 2;
        $values[] = 4;
        $values[1];
    }

    private static function testBriefValueBuiltInFuncBug($split)
    {
        $result = [
            'type' => 'flight',
            'couponNumber' => intval($split['D']),
            'from' => intval($split['F']),
            'to' => intval($split['T']),
        ];
        $result['']; // 'couponNumber' brief value should be `intval($split['D'])`, not `function intval($var, $base = null) {}`
    }

    private static function wrapClosure(callable $func)
    {
        return function(...$args) use ($func) {return $func(...$args);};
    }

    private function testFuncArrGoToDecl()
    {
        $funcs = [static::class, 'testBriefValueBuiltInFuncBug'];
        $funcs = [self::class, 'makeCoolOutfit'];
        $funcs = [$this, 'provideFuncVarUsageBasedCompletionFp'];
        static::wrapClosure([$this, 'makeRecordMaybe']);
    }

    /** pressing _Tools -> deep-assoc-completion -> To N-th Test_ and specifying test number (say 3) should get you to the 3-rd */
    public function testGetNthTestCase()
    {
        $list = [];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        $list[] = ['ASD ASD ASD ASD ', ['a' => 1, 'b' => 2]];
        return $list;
    }

    public function testSelfSuggestion()
    {
        /** @var $navCmdTypes = ['locator' => 'QWE123'] */
        $navCmdTypes = ['a' => 5, 'b' => 6, 'asd', '']; // should not suggest numbers
    }

    /**
     * @param stdClass $argRow {
     *      @property int id some description
     *      @property string name some description
     *      @property \stdClass childPurchase {
     *          @property int id some description
     *          @property float price
     *      }
     * }
     */
    public function testStdClassDoc($argRow)
    {
        /**
         *  @var stdClass $row {
         *       @property int id some description
         *       @property string name some description
         *       @property stdClass childPurchase {
         *           @property int id some description
         *           @property float price
         *       }
         *}
         **/
        while ($row = $result->fetch_object()) {
            $row->childPurchase->price;
        }
    }

    private static function testArrayKeysGoTo()
    {
        $arr = ['itinerary' => ['KIVKBP', 'KBPRIX'], 'paxes' => ['Vova', 'Petja']];
        $arr[''];
        $keys = array_keys($arr);
        $newArr = [];
        foreach ($keys as $key) {
            $newArr[$key] = 'new value';
        }
        $newArr['paxes'];
    }

    private static function getGalileoPnr()
    {
        $data = json_decode(file_get_contents('http://apollo.com/pnr/QWE123'), true);
        return [
            'recordLocator' => $data['recordLocator'],
            'paxName' => $data['paxName'],
            'itinerary' => $data['itinerary'],
            'price' => $data['price'],
        ];
    }

    private static function getSabrePnr()
    {
        $data = json_decode(file_get_contents('http://sabre.com/pnr/QWE123'), true);
        return [
            'recordLocator' => $data['recordLocator'],
            'paxName' => $data['paxName'],
            'itinerary' => $data['itinerary'],
            'price' => $data['price'],
        ];
    }

    private static function getAmadeusPnr()
    {
        $data = json_decode(file_get_contents('http://amadues.com/pnr/QWE123'), true);
        return [
            'recordLocator' => $data['recordLocator'],
            'paxName' => $data['paxName'],
            'itinerary' => $data['itinerary'],
            'price' => $data['price'],
        ];
    }

    /** @param $pnrs = [
     *     static::getGalileoPnr(),
     *     static::getSabrePnr(),
     *     static::getAmadeusPnr(),
     * ] */
    private static function testIteratorResolution($pnrs)
    {
        $pnr = $pnrs[rand()];
        $pnr[''];
    }

    private function testBuiltInCompletion()
    {
        $key = 123;
        $zhopa = [];
        $zhopa[];
        $GLOBALS[];
    }

    /**
     * @param $params = [
     *     'weekday' => self::we, // should suggest "weekday"
     *     'huikday' => YakumoRan::norm, // should suggest "normalizeShikigami"
     *     'youkai' => new Yaku, // should suggest "YakumoRan"
     * ]
     * @param $params2 = self::weekday(),
     * @param $params3 = new YakumoRan(),
     * @return array [
     *     'pnr' => self::getSabrePnr(), // should suggest "getSabrePnr"
     *     'maiden' => new ReimuHa, // should suggest "ReimuHakurei"
     * ]
     * @param $zhopa int asdsad kamsdk nasjkndjksand
     */
    private function testMultilineDoc($params, $params2, $params3)
    {
        $params[''];
    }

    private function testExtract()
    {
        $pax = ['name' => 'Vova', 'age' => 15, 'gender' => 'M'];
        extract($pax);
        // should suggest: name, age, gender
        $age;
        extract($pax, null, 'somePrefix_');
        // should suggest: somePrefix_name, somePrefix_age, somePrefix_gender
        $somePrefix_gender;
    }

    /** @param $sale = [
     *     'netPrice' => '500.89' , // price without taxes
     *     // or 'ticket', 'insurance', 'tablet'
     *     'product' => 'car',
     *     // the person that gives you the money
     *     // maybe a man, a woman, or a programmer
     *     'customer' => [
     *         'lastName' => 'Pupkin',
     *         'firstNme' => 'Vasya',
     *     ],
     * ] */
    private function testKeyDocs($sale)
    {
        // should include the comment in completion popup
        $sale[''];
        $hale = [
            'netPrice' => '500.89', // price without taxes actually very long comment
            // or 'ticket', 'insurance', 'tablet'
            'product' => 'car' ,
            // the person that gives you the money
            // may be a man, a woman, or a programmer
            'customer' => [
                'lastName' => 'Pupkin',
                'firstNme' => 'Vasya',
            ],
        ];
        $hale[''];
        $car = [
            'model' => 'Mitsubishi G199', /* cars with same model are usually same */
            /**
             * consists of letters that describe car features
             * like conditioning, wheel/door count, etc...
             */
            'carCode' => 'ECAR',
            'requiredLicense' => 'lightweight', // I never studied in driving school,
            // but I heard there is such stuff
            'sadad' => 123 // last element
        ];
        $car[''];
    }

    private static function makeDefaultSearchParams()
    {
        return ['asd' => '123', 'dsa' => 456];
    }

    private static function provideTypeHintedArrCreation($params)
    {
        /** @var $params = self::makeDefaultSearchParams() */
        $params = [
            // should suggest: "asd", "dsa"
            '' => 123,
        ];
    }

    //============================
    // not implemented follow
    //============================

    private static function testUndefinedKeyError()
    {
        $record = ['a' => 6, 'b' => 8];
        $records = [$record, $record];
        $records[0]['a'];

        // should show error like "Key 'someNotExistingKey' is not defined"
        print($record['someNotExistingKey']);
        print($record['huj']);
        print($record['c']);
    }

    private static function testArgumentCompatibilityError()
    {
        $records = [];
        $records[] = [
            'key1' => 15,
            'key2' => 15,
        ];

        $mapping = function($record) {
            // should not suggest anything
            $record[''];
            return $record['key1'] + $record['key2'] + $record['key3'];
        };
        // should report error since $records elements
        // don't have 'key3' used in the function
        $mapped = array_map($mapping, $records);
    }


    /**
     * @param array $attributes = [
     *     'namespace' => '',
     *     'slug' => '',
     *     'name' => 'Vasya',
     *     'is_localized' => false,
     *     'is_nullable' => false,
     *     'amount' => 50.00,
     *     'default_value' => null,
     *     'validation_rules' => [],
     *     'field_type' => \App\Enums\SettingFieldType::getInstance(),
     *     'options' => [
     *         'asd' => 123,
     *         'dsa' => 345,
     *     ],
     * ]
     * @param callable $callback
     */
    public static function group(array $attributes, callable $callback): void
    {
    }

    public static function doStuff()
    {
        static::group([
            // should not show _string_ in each key type
            '' => '',
        ]);
    }
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);

    $blockchain = [
        // ['type_text' => 'object', 'icon' => 'com.jetbrains.php.PhpIcons.VARIABLE', 'tail_text' => '->streams::addon.themes']
        'type' => 'ETH',
        'rpcUrl' => 'ololomainnet.com',
    ];
    // should use styling info from the key comment
    $blockchain[''];
}

main();

$sql = [];
// you should not get completion when you type 'dzhigurda', but you get it sadly...
$sql['dzigurasdv'][] = 123;

class get{
    public function random($array, $hujKey)
    {
        foreach($array as $var){
            $GLOBALS['insideFunc'] = 345;
            $GLOBALS[$var] = rand(1,100);
            $GLOBALS['ololo1223_date'] = new DateTime('now');
            $GLOBALS['ololo1223_tanya_the_evil'] = [
                'age' => 30,
                'gender' => 'male',
                'abilities' => ['magic', 'smart', 'evil'],
            ];
        }
    }
    private static function doHast()
    {
        $get = new get;
        $get->random(array('myVar2'), 'strArgKey2');
    }
}
$get = new get;
$get->random(array('myVar'), 'strArgKey');

$ololo1223_tanya_the_evil[''];
$ololo1223_date->;

$ololoNoMagic = [
    'base_url' => 'https://myawesomebestsiteevar.com',
    'db_password' => 'qwerty1223',
    'redis_password' => 'qwerty4321',
];
echo $myVar2;
echo $my;

class someSubclass
{
    public $x = 'rrr';
}

const ZHOPA = 123;
//define('ZHOPA', '123');

/**
 *  @var $row = (object)[
 *       'subName' => new \someSubclass,
 *       'childPurchase' => (object)[
 *           'id' => 123, // some description
 *           'price' => 13.50,
 *       ]
 * ]
 */
while($row = $result->fetch_object())
{
    $row->subName->;        // should suggest: x
    $row->childPurchase->;  // should suggest: id, price
}

/** @param $arg3 = ['obj' => (object)['a' => 5, 'b' => 6]] */
$doStuff = function($arg = ZHOPA, $arg2 = Plutia::TOY_NEP, $arg3){};
$doStuff(ZHOPA, '', ['']);

$toyType = Plutia::TOY_NEP;
if ($toyType === \NeptuniaNs\Plutia::TOY_NEP) {

}

/**
 * Initializes this class with the given options.
 *
 * @param array $options = [
 *     'required' => true, // Whether this element is required
 *     'label' => 'NET Price', // The display name for this element
 * ]
 */
function __construct(array $options = array())
{
    $options[''];
}

$arr = [];
$megami = ['a' => new Plutia()]['a'];
$arr[$megami->getName()] = 123;
$lagami = (object)['getLame' => 123];
$arr[$lagami->getLame];

$brothers = ['ololo', 'trololo'];
$brothers[1];

// should not suggest: 'hates', 'getAngry'
Blanc::;
(new Blanc())->;
$blancCls = Blanc::class;
$blancCls::;
(new $blancCls)->hates;

method_exists(new Blanc, '');

debug_backtrace();
json_encode([], );

stream_context_create([
    'http' => [
        '' => '',
    ],
]);

class A
{
    public function getFoo($key) {
        if ($key === 'qwrqwrqwr') {

        }
        return false;
    }
    /** @param $key = JSON_PRETTY_PRINT */
    public function getBar($key) {
        if ($key === JSON_UNESCAPED_SLASHES) {

        }
        return false;
    }
}

$a = new A();
$a->getFoo('');
$a->getBar(JSON_PRETTY_PRINT);

class BasePlugin
{
    /**
     * @var array = [$name => ['uses' => '', 'action' => '']]
     */
    public $routes = [];
}
$p = new BasePlugin;
$p->routes['asdfasdf']['action']; // completion works

class MyPlugin extends BasePlugin
{
    public $routes = [
        'a' => [
            // mahalai-mahalai, completion works now!
            '' => 'ololo',
        ]
    ];
}

['asd' => 123, 'dsa' => 456][''];
