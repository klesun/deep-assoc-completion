<?php
use DeepTest\TestData;
use Lib\Utils\Fp;

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

    private static function testArrMethRef()
    {
        // should suggest: "bombTransmutation", "sheerHeartAttack", "bitesZaDusto"
        array_map([\DeepTest\KiraYoshikage::class, ''], [1,2,3]);

        $kira = new \DeepTest\KiraYoshikage();
        // should suggest: "murder"
        $murderedNumbers = array_map([$kira, ''], [1,2,3]);
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

    public function provideFuncVarUsageBasedCompletion()
    {
        $list = [];

        $pccRecords = [
            ['gds' => 'apollo', 'pcc' => '1O4K'],
            ['gds' => 'sabre', 'pcc' => '611F'],
            ['gds' => 'amadeus', 'pcc' => 'RIX123456'],
        ];
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

    //============================
    // not implemented follow
    //============================

    private static function testUsedKeysInAVar()
    {
        $params = [
            // should suggest: "docType", "docCountry", "docNumber", "gender", "dob", "lastName", "firstName"
            '' => 123,
        ];
        $cmd = self::makeAddSsrCmd($params);
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
        $record[''];

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
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);
}

main();