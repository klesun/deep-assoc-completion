<?php
use DeepTest\TestData;

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

    //============================
    // not implemented follow
    //============================

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
}

function main()
{
    $zhopa = ['a' => 5, 'b' => 6, 'c' => ['d' => 7, 'e' => 8]];
    print($zhopa['a']);
}

main();