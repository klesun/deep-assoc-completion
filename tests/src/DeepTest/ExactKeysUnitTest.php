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
    private static function getPnrSchema()
    {
        return new DictP([], [
            'recordLocator' => new StringP([], []),
            'passengers' => new ListP([], ['elemType' => new DictP([], [
                'lastName' => new StringP([], []),
                'firstName' => new StringP([], []),
            ])]),
            'itinerary' => new ListP([], ['elemType' => new ListP([], [
                'from' => new StringP([], []),
                'to' => new StringP([], []),
                'date' => new StringP([], []),
            ])]),
        ]);
    }

    /** @param $pnr = ParamUtil::sample(static::getPnrSchema()) */
//    public static function provideParamValidation($pnr)
//    {
//        // should suggest: 'recordLocator', 'passengers', 'itinerary'
//        // should not suggest: 'elemType'
//        $pnr[''];
//        return [
//            [$pnr, ['recordLocator', 'passengers', 'itinerary']],
//        ];
//    }
}