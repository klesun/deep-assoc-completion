<?php

class Fp
{
    public static function map(callable $f, array $iterable)
    {
        return array_map($f, $iterable);
    }
}

class DeepKeysTest
{
    private static function makeRecord()
    {
        $record = [
            'id' => 123,
            'firstName' => 'Vasya',
            'lastName' => 'Pupkin',
            'year' => 2,
            'faculty' => 'programming',
            'pass' => [
                'birthDate' => '1995-09-01',
                'birthCountry' => 'Latvia',
                'passCode' => '123ABC',
                'expirationDate' => '2021-01-01',
                'family' => [
                    'spouse' => 'Perpetuya Pupkina',
                    'children' => [
                        'Valera Orlov',
                    ],
                ],
            ],
            'chosenSubSubjects' => array_map(function($i) {
                return [
                    'name' => 'philosophy_'.$i,
                    'priority' => 5.7 + $i,
                ];
            }, range(0,9)),
        ];
        $record['friends'][] = [
            'name' => 'Madao',
            'occupation' => 'Madao',
        ];
        $record['friends'][] = [
            'name' => 'Phoenix Wright',
            'occupation' => 'Madao',
        ];

        $record['chosenSubSubjects'][0]['name'];

        return $record;
    }

    function makeRecordMaybe()
    {
        return rand() < 0.5 ? self::makeRecord() : null;
    }

    private static function testSimple()
    {
        $denya = self::makeRecord();
        // should suggest: id, firstName,
        // lastName, year, faculty, chosenSubSubjects
        print($denya['pass']);
        // should suggest same
        self::makeRecord()[''];
        print($denya['friends'][0]['name']);
    }

    private static function testScopes()
    {
        $denya = self::makeRecord();
        if (rand() > 0.5) {
            $denya = ['randomDenya' => -100];
            // should suggest _only_ randomDenya
            $denya[''];
        } elseif (rand() > 0.5) {
            $denya = ['randomDenya2' => -100];
            // should suggest _only_ randomDenya2
            $denya[''];
        }
        // should suggest all keys from makeRecord(),
        // 'randomDenya' and 'randomDenya2'
        // (preferably highlighted in different collors)
        print($denya['']);

        $denya = ['thisKeyWillNotBeSuggested' => 1414];
    }

    private static function testElseIfAssignment()
    {
        if ($res = ['a' => 1]) {
            // should suggest only a
            $res[''];
        } elseif ($res = ['b' => 2]) {
            $res['roro'] = 'asdasd';
            // should suggest only b and asdasd
            $res[''];
        } elseif ($res = ['c' => 3]) {
            // should suggest only c
            $res[''];
        } else if ($res = ['d' => 4]) {
            // should suggest only d
            $res[''];
        }
        // should suggest a,b,c.d
        $res[''];
    }

    private static function testKeyAssignment()
    {
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
                $record[''];
            }
        }

        // should suggest initialKey, dynamicKey1, dynamicKey2
        $record['dynamicKey1'];
    }

    private static function testKeyKeyAccess()
    {
        $record = self::makeRecord();
        // should suggest birthDate, birthCountry,
        // passCode, expirationDate, family
        $record['pass'][''];
        $family = $record['pass'][''];
        // should suggest spouse, children
        $family['children'];
    }

    private static function testBasisListAccess()
    {
        // should suggest name, priority
        self::makeRecord()['chosenSubSubjects'][4][''];

        $makeTax = function($i) {
            return [
                'currency' => -'USD',
                'amount' => 199 + $i,
            ];
        };
        $mapped = \array_map($makeTax, [1,2,3]);
        // should suggest currency, amount
        $mapped[0][''];

        return $mapped;
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

    private static function testArrayAppendInference()
    {
        $records = [];

        for ($i = 0; $i < 10; ++$i) {
            $records[] = [
                'id' => $i,
                'randomValue' => rand(),
                'origin' => 'here',
            ];
        }
        // should suggest id, randomValue, origin
        $records[0][''];

        $lala = [];
        $lala[0]['asdas'][] = [
            'id' => -100,
            'randomValue' => rand(),
            'origin' => 'there',
            'originData' => [1,2,3],
        ];
        $lolo = $lala;
        // should suggest asdas
        $lolo[0]['asdas'][0][''];
        // should suggest id, randomValue, origin, originData
        $lolo[0]['asdas'][4]['originData'];
    }

    private static function testNullKeyAccess()
    {
        $record = [
            'a' => 5,
            'b' => null,
            'c' => null,
            'd' => 7,
        ];
        // should suggest a,b,c,d
        $record[''];
    }

    private static function testTernaryOperator()
    {
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
        $record['b'][''];
        // should suggest trueKeyC, falsephpstormKeyC
        $record['c'][''];
    }

    private static function testNullCoalesce()
    {
        $maybeRecord = null
            ?? self::makeRecordMaybe()
            ?? self::makeRecordMaybe()
            ?? ['error' => 'maybe no']
        ;

        // should suggest all from makeRecord() and error
        $maybeRecord['pass                                                                                                                                                                                      '];
    }

    /**
     * @param $a = ['key1' => 5, 'hey2' => 6]
     * @param $b = [
     *     'nestedAssoc' => [
     *         'nestedKey1' => 213,
     *         'nestedKey2' => 213,
     *         'nestedKey3' => 213,
     *     ],
     *     'numbers' => [1,5,3,6],
     * ]
     * @param $c = DeepKeysTest::makeRecord()
     */
    private static function testDocHint($a, $b, $c)
    {
        // should suggest: 'key1', 'key2'
        /** @var $a array - hujna */
        $a[''];
        // should suggest: 'nestedAssoc', 'numbers'
        $b[''];
        // should suggest: 'nestedKey1', 'nestedKey2', 'nestedKey3'
        $b['nestedAssoc'][''];
        // should suggest makeRecord keys
        $c[''];
    }

    //============================
    // not implemented follow
    //============================

    private static function testTuples()
    {
        $musician = ['genre' => 'jass', 'instrument' => 'trumpet'];
        $programmer = ['language' => 'C#', 'orientation' => 'backend'];
        $teacher = ['subject' => 'history', 'students' => 98];

        $tuple = [$musician, $programmer, $teacher];
        // should suggest genre, instrument
        $tuple['0'][''];
        // should suggest language, orientation
        $tuple['1'][''];
        // should suggest subject, students
        $tuple['2'][''];

        list($mus, $prog, $tea) = $tuple;
        // should suggest what should be suggested
        $mus[''];
        $prog[''];
        $tea[''];
    }

    private static function testForeachAccess()
    {
        $record = self::makeRecord();

        $subjects = $record['chosenSubSubjects'];

        foreach ($subjects as $subject) {
            // should suggest name, priority
            $subject[''];
        }
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


        $fpMapped = Fp::map(function($i) {
            return [
                'id' => $i,
                'amount' => 10.00,
                'date' => date('Y-m-d H:i:s'),
            ];
        }, range(0,10));

        // should suggest id, amount, date
        $fpMapped[0][''];
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

    private static function testUndefinedKeyError()
    {
        $record = ['a' => 6, 'b' => 8];
        // should show error like "Key 'someNotExistingKey' is not defined"
        print($record['someNotExistingKey']);
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
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);
}

main();