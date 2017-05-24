<?php
namespace DeepTest;

/**
 * TODO: make somehow possible to say that aray
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
     * @param array $secondBomb = \DeepTest\KiraYoshikage::sheerHeartAttack()
     * @param array $thirdBomb = KiraYoshikage::bombTransmutation()
     */
    public static function provideForeignFileInDoc($secondBomb, $thirdBomb)
    {
        $list = [];

        $list[] = [$secondBomb, ['veryTough' => [], 'smallCar' => ['that' => [], 'follows' => []]]];

        // should suggest all the keys from the function
        $list[] = [$thirdBomb['touch'], ['into' => [], 'a' => [], 'bomb' => []]];

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

    private static function makeKonohaCitizen(): IKonohaCitizen
    {
        if (rand() % 2) {
            return Naruto::kageBunshin();
        } else {
            return new Konohamaru();
        }
    }

    public function provideInstanceMethod()
    {
        $list = [];

        // not implemented yet

        $bunshin = Naruto::kageBunshin();
        $money = $bunshin->payForDinner(100);
        // should suggest: "currency", "amount"
        $list[] = [$money, ['currency' => [], 'amount' => []]];

        // it should work. i guess i just return null somewhere
        // in code when function is resolved in multiple classes
        $konohanian = self::makeKonohaCitizen();
        $taxBundle = $konohanian->payTaxes();
        // TODO: uncomment!
        // should suggest either from doc in interface or from implementations
//        $list[] = [$taxBundle, ['currency' => [], 'incomeTax' => [], 'gamblingTax' => [], 'familyTax' => []]];

        return $list;
    }

    public function provideVeryDeepKey()
    {
        $list = [];

        // not implemented yet

        // ideally, limit should be some ridiculously big number
        // so you would never reach it in normal circumstances
        // that requires handling circular references properly

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