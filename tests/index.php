<?php

class DeepKeysTest
{
    private static function makeRecord()
    {
        return [
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
                    'spouse' => 'Perpetuya Orlova',
                    'children' => [
                        'Valera Orlov',
                    ],
                ],
            ],
            'chosenSubSubjects' => [
                [
                    'name' => 'philosophy',
                    'priority' => 5.7,
                ],
                [
                    'name' => 'micro-controllers',
                    'priority' => 4.1,
                ],
                [
                    'name' => 'french',
                    'priority' => 3.2,
                ],
            ],
        ];
    }

    private static function testSimple()
    {
        $denya = self::makeRecord();
        // should suggest: id, firstName,
        // lastName, year, faculty, chosenSubSubjects
        print($denya['']);
        // should suggest same
        self::makeRecord()[''];
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
        }
        // should suggest a,b,c
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
        $record[''];
    }

    private static function testKeyKeyAccess()
    {
        $record = self::makeRecord();
        // should suggest birthDate, birthCountry,
        // passCode, expirationDate, family
        $record['pass'][''];
        $family = $record['pass']['family'];
        // should suggest spouse, children
        $family[''];
    }

    //============================
    // not implemented follow
    //============================

    private static function testListAccess()
    {
        $subjects = self::makeRecord()['chosenSubSubjects'];
        // should suggest name, priority
        $subjects[1][''];

        foreach ($subjects as $subject) {
            // should suggest name, priority
            $subject[''];
        }
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

    private static function testUndefinedKeyError()
    {
        $record = ['a' => 6, 'b' => 8];
        // should show error like "Key 'someNotExistingKey' is not defined"
        print($record['someNotExistingKey']);
    }
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);
}

main();