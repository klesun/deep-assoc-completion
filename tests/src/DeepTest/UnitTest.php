<?php
namespace DeepTest;

use TouhouNs\ReimuHakurei;

/**
 * TODO: make somehow possible to say that array
 * must have _only_ the keys from the expected output
 */
class UnitTest /** extends \PHPUnit_Framework_Suite */
{
    public static function provideSimpleTest()
    {
        $list = [];

        // from function
        $list[] = [
            TestData::makeStudentRecord(),
            ['id' => [], 'firstName' => [], 'lastName' => [], 'year' => [], 'faculty' => [], 'chosenSubSubjects' => []],
        ];

        // from var
        $denya = TestData::makeStudentRecord();
        $list[] = [
            $denya,
            ['id' => [], 'firstName' => [], 'lastName' => [], 'year' => [], 'faculty' => [], 'chosenSubSubjects' => []],
        ];

        // from inner key
        $list[] = [
            $denya['friends'][0],
            ['name' => [], 'occupation' => []],
        ];

        return $list;
    }

    public static function provideTestScopes()
    {
        $list = [];

        $denya = TestData::makeStudentRecord();
        if (rand() > 0.5) {
            $denya = ['randomDenya' => -100];
            // should suggest _only_ randomDenya
            $list[] = [$denya, ['randomDenya' => []]];
        } elseif (rand() > 0.5) {
            $denya = ['randomDenya2' => -100];
            // should suggest _only_ randomDenya2
            $list[] = [$denya, ['randomDenya2' => []]];
        }
        // should suggest all keys from makeRecord(),
        // 'randomDenya' and 'randomDenya2'
        // (preferably highlighted in different collors)
        $list[] = [$denya, TestData::makeStudentRecord()];
        $list[] = [$denya, ['randomDenya' => [], 'randomDenya2' => []]];

        $denya = ['thisKeyWillNotBeSuggested' => 1414];


        return $list;
    }

    public static function provideTestElseIfAssignment()
    {
        $list = [];

        if ($res = ['a' => 1]) {
            // should suggest only a
            $list[] = [$res, ['a' => []]];
        } elseif ($res = ['b' => 2]) {
            $res['roro'] = 'asdasd';
            // should suggest only b and asdasd
            $list[] = [$res, ['b' => [], 'roro' => []]];
        } elseif ($res = ['c' => 3]) {
            // should suggest only c
            $list[] = [$res, ['c' => []]];
        } else if ($res = ['d' => 4]) {
            // should suggest only d
            $list[] = [$res, ['d' => []]];
        }
        // should suggest a,b,c.d
        $list[] = [$res, ['a' => [], 'b' => [], 'roro' => [], 'c' => [], 'd' => []]];

        return $list;
    }

    public static function provideTestKeyAssignment()
    {
        $list = [];

        $record = ['initialKey' => 123];
        if (rand() > 0.5) {
            $record = ['initialKey2' => 123];
            if (rand(0.5) > 0.5) {
                $record['dynamicKey1'] = 234;
            }
        } else {
            if (rand(0.5) > 0.5) {
                $record['dynamicKey2'] = 345;
                // must not contain dynamicKey1 and initialKey2
                $list[] = [$record, ['initialKey' => [], 'dynamicKey2' => []]];
            }
        }

        // should suggest initialKey, dynamicKey1, dynamicKey2
        $list[] = [$record, [
            'initialKey' => [], 'initialKey2' => [],
            'dynamicKey1' => [], 'dynamicKey2' => []]
        ];

        return $list;
    }

    public static function provideTestKeyKeyAccess()
    {
        $list = [];

        $record = TestData::makeStudentRecord();
        // should suggest birthDate, birthCountry,
        // passCode, expirationDate, family
        $list[] = [$record['pass'], [
            'birthDate' => [], 'birthCountry' => [],
            'passCode' => [], 'expirationDate' => [], 'family' => [],
        ]];
        $family = $record['pass']['family'];
        // should suggest spouse, children
        $list[] = [$family, ['spouse' => [], 'children' => []]];

        return $list;
    }

    public static function provideTestBasisListAccess()
    {
        $list = [];

        // should suggest name, priority
        $list[] = [
            TestData::makeStudentRecord()['chosenSubSubjects'][4],
            ['name' => [], 'priority' => []],
        ];

        $makeTax = function($i) {
            return [
                'currency' => -'USD',
                'amount' => 199 + $i,
            ];
        };
        $mapped = \array_map($makeTax, [1,2,3]);
        // should suggest currency, amount
        $mapped[0][''];
        $list[] = [$mapped[0], ['currency' => [], 'amount' => []]];

        return $list;
    }

    public static function provideTestArrayAppendInference()
    {
        $list = [];

        $records = [];

        for ($i = 0; $i < 10; ++$i) {
            $records[] = [
                'id' => $i,
                'randomValue' => rand(),
                'origin' => 'here',
            ];
        }
        // should suggest id, randomValue, origin
        $list[] = [$records[0], ['id' => [], 'randomValue' => [], 'origin' => []]];

        $lala = [];
        $lala[0]['asdas'][] = [
            'id' => -100,
            'randomValue' => rand(),
            'origin' => 'there',
            'originData' => [1,2,3],
        ];
        $lolo = $lala;
        // should suggest asdas
        $list[] = [$lolo[0], ['asdas' => []]];
        // should suggest id, randomValue, origin, originData
        $list[] = [$lolo[0]['asdas'][4], [
            'id' => [], 'randomValue' => [],
            'origin' => [], 'originData' => [],
        ]];

        return $list;
    }

    public static function provideTestNullKeyAccess()
    {
        $list = [];

        $record = [
            'a' => 5,
            'b' => null,
            'c' => null,
            'd' => 7,
        ];
        // should suggest a,b,c,d
        $list[] = [$record, ['a' => [], 'b' => [], 'c' => [], 'd' => []]];

        return $list;
    }

    public static function provideTestTernaryOperator()
    {
        $list = [];

        $record = [
            'a' => 5,
            'b' => rand() > 0.5 ? [
                'trueKeyB' => 5,
            ] : [
                'falseKeyB' => 5,
            ],
        ];
        $record['c'] = rand() > 0.5 ? [
            'trueKeyC' => 5,
        ] : [
            'falseKeyC' => 5,
        ];

        // should suggest trueKeyB, falseKeyB
        $list[] = [$record['b'], ['trueKeyB' => [], 'falseKeyB' => []]];
        // should suggest trueKeyC, falsephpstormKeyC
        $list[] = [$record['c'], ['trueKeyC' => [], 'falseKeyC' => []]];

        return $list;
    }

    public static function provideTestNullCoalesce()
    {
        $list = [];

        $maybeRecord = null
            ?? TestData::makeStudentRecord()
            ?? TestData::makeStudentRecord()
            ?? ['error' => 'maybe no']
        ;

        // should suggest all from makeRecord() and error
        $list[] = [$maybeRecord, ['error' => []]];
        $list[] = [$maybeRecord, TestData::makeStudentRecord()];

        return $list;
    }

    /**
     * @param $a = ['key1' => 5, 'key2' => 6]
     * @param $b = [
     *     'nestedAssoc' => [
     *         'nestedKey1' => 213,
     *         'nestedKey2' => 213,
     *         'nestedKey3' => 213,
     *     ],
     *     'numbers' => [1,5,3,6],
     * ]
     * @param $c = TestData::makeStudentRecord()
     */
    public static function provideDocHint($a, $b, $c)
    {
        $list = [];

        // should suggest: 'key1', 'key2'
        $list[] = [$a, ['key1' => [], 'key2' => []]];
        // should suggest: 'nestedAssoc', 'numbers'
        $list[] = [$b, ['nestedAssoc' => [], 'numbers' => []]];
        // should suggest: 'nestedKey1', 'nestedKey2', 'nestedKey3'
        $list[] = [$b['nestedAssoc'], [
            'nestedKey1' => [], 'nestedKey2' => [], 'nestedKey3' => [],
        ]];
        // should suggest makeRecord keys
        $list[] = [$c['pass'], TestData::makeStudentRecord()['pass']];

        return $list;
    }

    /**
     * @param array $firstBomb = \DeepTest\KiraYoshikage::bombTransmutation()
     * @param array $secondBomb = KiraYoshikage::sheerHeartAttack()
     * @param array $secondBomb = ReimuHakurei::fantasySeal()
     * @param $thirdBomb = [
     *     'name' => 'Bites The Dust',
     *     'castRange' => 2.5,
     *     'power' => 999.99,
     *     'requirements' => [
     *         'desperation' => 0.99999,
     *         'magicArrows' => 1,
     *         'evilness' => 1.00,
     *     ],
     * ]
     */
    public static function provideForeignFileInDoc($firstBomb, $secondBomb, $thirdBomb)
    {
        $list = [];

        $list[] = [$secondBomb, ['veryTough' => [], 'smallCar' => ['that' => [], 'follows' => []]]];

        // should suggest all the keys from the function
        $list[] = [$firstBomb['touch'], ['into' => [], 'a' => [], 'bomb' => []]];

        return $list;
    }

    public static function provideForeachAccess()
    {
        $list = [];

        $record = TestData::makeStudentRecord();
        foreach ($record['chosenSubSubjects'] as $subject) {
            // should suggest name, priority
            $subject['priority'];
            $list[] = [$subject, ['name' => [], 'priority' => []]];
        }

        return $list;
    }

    public static function provideTupleDirectAccess()
    {
        $list = [];

        $simpleTuple = [
            ['a' => 5, 'b' => 6],
            ['a' => 5, 'b' => 6],
            'huj' => 'asd',
        ];
        // should suggest: "0", "1", "huj"
        $list[] = [$simpleTuple, ['0' => [], '1' => [], 'huj']];
        // should suggest: "a", "b"
        $list[] = [$simpleTuple['0'], ['a' => [], 'b' => []]];
        // should suggest: "a", "b"
        $list[] = [$simpleTuple[1], ['a' => [], 'b' => []]];

        return $list;
    }

    public static function provideTuples()
    {
        $list = [];

        $musician = ['genre' => 'jass', 'instrument' => 'trumpet'];
        $programmer = ['language' => 'C#', 'orientation' => 'backend'];
        $teacher = ['subject' => 'history', 'students' => 98];

        $tuple = [$musician, $programmer, $teacher];
        // should suggest genre, instrument
        $list[] = [$tuple['0'], ['genre' => [], 'instrument' => []]];
        // should suggest language, orientation
        $list[] = [$tuple['1'], ['language' => [], 'orientation' => []]];
        // should suggest subject, students
        $list[] = [$tuple['2'], ['subject' => [], 'students' => []]];

        list($mus, $prog, $tea) = $tuple;
        // should suggest what should be suggested
        $list[] = [$mus, ['genre' => [], 'instrument' => []]];
        $list[] = [$prog, ['language' => [], 'orientation' => []]];
        $list[] = [$tea, ['subject' => [], 'students' => []]];

        return $list;
    }

    public static function providePregMatch(string $line)
    {
        $list = [];

        $regex =
            '/^\s*'.
            '(?P<segmentNumber>\d+)\s+'.
            '(?P<airline>[A-Z0-9]{2})\s*'.
            '(?P<flightNumber>\d{1,4})\s*'.
            '(?P<bookingClass>[A-Z])'.
            '/';

        // TODO: following causes dead loop for some reson - fix!
        if (preg_match($regex, $line, $matches)) {
            // should suggest: "segmentNumber", "airline", "flightNumber", "bookingClass"
            $matches[''];
            $list[] = [$matches, [
                'segmentNumber' => [], 'airline' => [],
                'flightNumber' => [], 'bookingClass' => [],
            ]];
        }

        return $list;
    }

    public static function provideBuiltIns()
    {
        $list = [];

        $records = array_map(function($i){return [
            'type' => 'generated',
            'score' => rand(0,100),
            'student' => 'Vasya',
            'parsed' => [
                'id' => $i,
                'generationTime' => rand(0,10),
            ],
        ];}, range(0,10));
        $records[] = [
            'type' => 'mostAverage',
            'score' => 54,
            'student' => 'Vova',
            'parsed' => [
                'comment' => 'two units higher than a dog',
                'averageness' => 'averagelyAverage',
            ],
        ];
        $records[] = [
            'type' => 'mostBlonde',
            'score' => 52,
            'student' => 'Nastya',
            'parsed' => [
                'comment' => 'blonde soul can\'t be dyed',
                'blondeness' => 'veryBlonde',
            ],
        ];

        // all following should suggest: "id", "score", "student"
        $list[] = [$records[0], [
            'type' => [], 'score' => [], 'student' => [], 'parsed' => [],
        ]];
        $list[] = [
            array_pop($records),[
            'type' => [], 'score' => [], 'student' => [], 'parsed' => [],
        ]];
        $list[] = [
            array_shift($records),[
            'type' => [], 'score' => [], 'student' => [], 'parsed' => [],
        ]];
        $list[] = [
            array_reverse($records)[0],[
            'type' => [], 'score' => [], 'student' => [], 'parsed' => [],
        ]];

        $byType = array_combine(
            array_column($records, 'type'),
            array_column($records, 'parsed')
        );
        // should suggest: "id", "generationTime"
        $list[] = [
            $byType['generated'],
            ['id' => [], 'generationTime' => []],
        ];
        // should suggest: "averageness"
        $byType['mostAverage'][''];
        $list[] = [
            $byType['mostAverage'],
            ['averageness' => []],
        ];
        // should suggest: "blondeness"
        $list[] = [
            $byType['mostBlonde'],
            ['blondeness' => []],
        ];

        return $list;
    }

    private static function makeKonohaCitizen()
    {
        if (rand() % 2) {
            $konohanian = Naruto::kageBunshin();
        } else {
            $konohanian = new Konohamaru();
        }
        return $konohanian;
    }

    public function provideInstanceMethod()
    {

        $bunshin = Naruto::kageBunshin();
        $money = $bunshin->payForDinner(100);
        // should suggest: "currency", "amount"
        $list[] = [$money, ['currency' => [], 'amount' => []]];

        $konohanian = self::makeKonohaCitizen();
        $taxBundle = $konohanian->payTaxes();

        // should suggest from all implementations
        $taxBundle['incomeTax'];
        $list[] = [$taxBundle, ['currency' => [], 'incomeTax' => [], 'gamblingTax' => [], 'familyTax' => []]];

        return $list;
    }

    private static function makeKonohanianIface(): IKonohaCitizen
    {
        return self::makeKonohaCitizen();
    }

    public function provideInterfaceMethod(IKonohaCitizen $randomGuy)
    {
        $list = [];

        $list[] = [$randomGuy->payTaxes(), ['currency' => [], 'incomeTax' => [], 'gamblingTax' => [], 'familyTax' => []]];

        $konohanian = self::makeKonohanianIface();
        $taxBundle = $konohanian->payTaxes();
        // should suggest either from doc in interface or from implementations
        $list[] = [$taxBundle, ['currency' => [], 'incomeTax' => [], 'gamblingTax' => [], 'familyTax' => []]];

        return $list;
    }

    public function provideArrayChunk()
    {
        $list = [];

        $vova = ['occupation' => 'salesman', 'salary' => '300'];
        $nastya = ['occupation' => 'hooker', 'salary' => '900'];
        $igorj = ['occupation' => 'pudge', 'salary' => '200'];
        $katja = ['occupation' => 'singer', 'salary' => '900'];

        $workers = [$vova, $nastya, $katja, $igorj];
        $pairs = array_chunk($workers, 2);
        foreach ($pairs as $pair) {
            $list[] = [$pair[0], ['occupation' => [], 'salary' => []]];
        }

        return $list;
    }

    private static function makeBarrel(int $i)
    {
        return [
            'material' => [
                0 => 'oak',
                1 => 'christmas tree',
                2 => 'bamboo',
            ][rand(0,3)],
            'radius' => rand(0,10),
            'daughter' => 'Amane Suzuha',
        ];
    }

    public function provideMethByRef()
    {
        $list = [];

        // array_map with inline closure
        $ingredients = array_map(function($name){return [
            'name' => $name,
            'amount' => strlen($name),
        ];}, ['tomato', 'cucumber', 'pepper']);
        $list[] = [$ingredients[2], ['name' => [], 'amount' => []]];

        // with closure in a variable
        $makeSnowman = function(){return [
            'headSize' => rand(0,10),
            'torsoSize' => rand(10,20),
            'legsSize' => rand(20,30),
        ];};
        $snowmen = array_map($makeSnowman, range(1,10));
        $list[] = [$snowmen[4], ['headSize' => [], 'torsoSize' => [], 'legsSize' => []]];

        $barrels = array_map([self::class, 'makeBarrel'], [0,1,2,3,4]);
        $list[] = [$barrels[2], ['material' => [], 'radius' => [], 'daughter' => []]];

        $bombs = array_map([ReimuHakurei::class, 'evilSealingCircle'], [0,1,2,3,4]);
        $list[] = [$bombs[2], ['missileDensity' => [], 'missileDamage' => [], 'arcDegree' => []]];

        return $list;
    }

    /** @return array like [
     *     ['index' => 1, 'value' => 1, 'time' => 0.002],
     *     ['index' => 2, 'value' => 1, 'time' => 0.004],
     *     ['index' => 3, 'value' => 2, 'time' => 0.008],
     *     ...
     * ] */
    private static function fibonacci(int $n)
    {
        if ($n <= 0) {
            $result = [];
        } elseif ($n === 1) {
            $result = [];
            $result[] = ['index' => $n, 'value' => 1, 'time' => 0.00];
        } else {
            $startTime = microtime();
            $result = self::fibonacci($n - 1);
            $value = $result[$n - 3]['value'] + $result[$n - 2]['value'];
            $result[] = ['index' => $n, 'value' => $value, 'time' => microtime() - $startTime];
        }
        return $result;
    }

    public function provideRecursiveFunc()
    {
        $list = [];

        // it would be perfect if plugin detected
        // that it is recursive function at once instead
        // of interrupting after reaching a certain depth
        $fiboRecs = self::fibonacci(10);
        $list[] = [$fiboRecs[0], ['index' => [], 'value' => [], 'time' => []]];

        return $list;
    }

    private function addFullDt($passedSeg)
    {
        $passedSeg['fullDt'] = date('Y-m-d H:i:s');
        return $passedSeg;
    }

    public function provideBasicGenericTyping()
    {
        $list = [];

        $seg = ['from' => 'KIV', 'to' => 'RIX'];
        $fullSeg = self::addFullDt($seg);
        $fullSeg[''];
        $list[] = [$fullSeg, ['from' => [], 'to' => [], 'fullDt' => []]];

        // apparently something wrong happens when name is same - should correct
        // var scope to not include assigned var to passed var resolutions
        $sfoSeg = ['from' => 'LON', 'to' => 'SFO', 'netPrice' => '240.00'];
        $sfoSeg = self::addFullDt($sfoSeg);
        $sfoSeg[''];
        $list[] = [$sfoSeg, ['from' => [], 'to' => [], 'netPrice' => [], 'fullDt' => []]];


        $denis = ['job' => false, 'girlfriend' => false];
        $denis = ['whiskey' => true, 'dota' => true, 'oldDenis' => $denis];
        $list[] = [$denis['oldDenis'], ['job' => [], 'girlfriend' => []]];

        return $list;
    }

    //=============================
    // following are not implemented yet
    //=============================

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

        $paxedItin = array_map([self::class, 'addPax'], $itinerary);
        $paxedItin[0][''];

        // TODO: uncomment when i figure out how to pass args to an expression returning a func
        //$list[] = [$seg, ['from' => [], 'to' => [], 'fullDt' => []]];
        //$list[] = [$datedItin[0], ['from' => [], 'to' => [], 'fullDt' => []]];
        //$list[] = [$paxedItin[0], ['from' => [], 'to' => [], 'fullDt' => [], 'pax' => []]];

        return $list;
    }

    public function provideVeryDeepKey()
    {
        $list = [];

        // not implemented yet

        // ideally, limit should be some ridiculously big number
        // so you would never reach it in normal circumstances,
        // but that requires handling circular references properly

        $addict = [
            'face' => [
                'eyes' => [
                    'left' => [
                        'pupil' => [
                            'color' => 'red',
                            'size' => [
                                'value' => 'veryBig',
                                'reason' => 'marijuana',
                            ],
                        ],
                    ],
                ],
            ],
        ];

        // should suggest all these keys by the wya
        $pupilSize = $addict['face']['eyes']['left']['pupil']['size'];
        // should suggest: "value", "reason"
        $list[] = [$pupilSize, ['value' => [], 'reason' => []]];

        $policeDepartment = [
            'evidenceOfTheYear' => $pupilSize,
            'offices' => [
                '402' => [
                    'evidenceOfTheChef' => $pupilSize,
                    'deskByTheWindow' => [
                        'dayShift' => [
                            'favouriteEvidence' => $pupilSize,
                            'cases' => [
                                '8469132' => [
                                    'mainEvidence' => $pupilSize,
                                    'evidences' => [$pupilSize],
                                ],
                            ],
                        ],
                    ],
                ],
            ],
        ];

        // should suggest: "value", "reason"
        $list[] = [
            $policeDepartment['evidenceOfTheYear'],
            ['value' => [], 'reason' => []]
        ];
        $list[] = [
            $policeDepartment['offices']['402']['evidenceOfTheChef'],
            ['value' => [], 'reason' => []]
        ];
        // following will fail till i fix circular references
        // TODO: uncomment!
//        $list[] = [
//            $policeDepartment['offices']['402']['deskByTheWindow']['dayShift']['favouriteEvidence'],
//            ['value' => [], 'reason' => []]
//        ];
//        $list[] = [
//            $policeDepartment['offices']['402']['deskByTheWindow']['dayShift']['cases']['8469132']['mainEvidence'],
//            ['value' => [], 'reason' => []]
//        ];
//        $list[] = [
//            $policeDepartment['offices']['402']['deskByTheWindow']['dayShift']['cases']['8469132']['evidences'][0],
//            ['value' => [], 'reason' => []]
//        ];

        return $list;
    }
}