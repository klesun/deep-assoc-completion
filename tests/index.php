<?php

use DeepTest\TestData;
use Illuminate\Database\Eloquent\Model, Lib\Utils\Fp;

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
    }

    private static function testLambdaAccess()
    {
        $subjects = self::makeRecord()['chosenSubSubjects'];
        $filtered = array_filter($subjects, function($subject) {
            // should suggest name, priority
            return $subject[''] > 4.0;
        });
        // should suggest name, priority
        $filtered[3][''];
        $mapped = array_map(function($subject) {
            return [
                // should suggest name, priority
                'name2' => $subject[''].'_2',
                'priority2' => $subject[''] + 4,
            ];
        }, $subjects);
        // should suggest name2, priority2
        $mapped[2][''];
    }

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
        array_key_exists('', $segments[0]);

        $pnrs = [];
        $pnrs[] = [
            'ptcInfo' => ['ptc' => 'C05', 'quantity' => 1],
            'paxes' => [['name' => 'vova', 'surname' => 'turman']],
            'itinerary' => $segments,
        ];
        array_key_exists('itinerary', $pnrs[0]);
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

    private static function testReverseType()
    {
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
        self::sendPqToGoogle([
            // should suggest: "gds", "pcc", "pnrDump", "pricingDump"
            '' => 'sabre',
        ]);
    }

    public function provideConstructorCompletion()
    {
        $marisa = new \TouhouNs\MarisaKirisame([
            // should suggest: "ability", "bombsLeft", "livesLeft", "power"
            '' => 'Master Spark',
        ]);
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

    private static function provideReplaceCompletion()
    {
        $testState = array_merge(self::makeDefaultApolloState(), [
            '' => true, // should suggest: gds, area, pcc, etc...
        ]);
        $destState = array_replace(self::makeDefaultApolloState(), [
            '' => true, // should suggest: gds, area, pcc, etc...
        ]);
    }

    private static function providePlusCompletion()
    {
        $testState = self::makeDefaultApolloState() + [
            '' => true, // should suggest: gds, area, pcc, etc...
        ];
    }

    private static function testTypeHintedArrCreation()
    {
        /** @var $params = ['asd' => '123', 'dsa' => 456] */
        $params = [
            // should suggest: "asd", "dsa"
            '' => 123,
        ];
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
        $result[''];
        $result['']; // 'couponNumber' brief value should be `intval($split['D'])`, not `function intval($var, $base = null) {}`
    }

    private static function testUsedKeysInAVar()
    {
        $params = [
            // should suggest: "docType", "docCountry", "docNumber", "gender", "dob", "lastName", "firstName"
            'gender' => 123,
        ];
        $cmd = self::makeAddSsrCmd($params);
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

    private static function testDeepUsageCompletion()
    {
        $availability = self::getAvailability([
            // should suggest: id, from, to
            '' => 123,
        ]);
    }

    private static function wrapClosure(callable $func)
    {
        return function(...$args) use ($func) {return $func(...$args);};
    }

    private function testFuncArrGoToDecl()
    {
        $funcs = [static::class, 'getAvailability'];
        $funcs = [self::class, 'makeCoolOutfit'];
        $funcs = [$this, 'provideFuncVarUsageBasedCompletionFp'];
        static::wrapClosure([$this, 'makeRecordMaybe']);
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

    private static function testLaravelModelCtorParams()
    {
        $competitor = new \App\Orm\Competitor([
            // should suggest: id, created_at, updated_at, spice_left
            '' => 123,
        ]);
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

    //============================
    // not implemented follow
    //============================

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

    /** @param $a = Asd::dsa(); */
    public function provideAbstractMethodUsedKeys()
    {
        Asd::dsa();
        $result = (new \Gtl\AmadeusSoapActions\AmadeusGetFareRulesAction('GENERIC'))
            ->execute([
                ''
            ]);
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

    private static function testUsedKeysPassedDeeper()
    {
        $hero = self::makeHero([
            // should suggest: "name", "enemyName", etc...
            '' => 'Bob',
            'outfitMaterials' => [
                // should suggest: "wood", "wool", "iron", etc...
                '',
            ],
            'underling' => self::spawnUnderling([
                // should suggest: "name", "enemyName", etc...
                '' => 'Jim',
            ]),
        ]);
    }

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

    private static function testArrayKeysGoTo()
    {
        $arr = ['itinerary' => ['KIVKBP', 'KBPRIX'], 'paxes' => ['Vova', 'Petja']];
        $keys = array_keys($arr);
        $newArr = [];
        foreach ($keys as $key) {
            $newArr[$key] = 'new value';
        }
        $newArr['itinerary'];
    }
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);
}

main();

$sql = [];
// you should not get completion when you type 'dzhigurda'
$sql['dzigurasdv'][] = 123;

