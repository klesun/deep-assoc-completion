<?php
namespace DeepTest;

/**
 * @property $reimuResult = Result::makeOk(new ReimuHakurei())
 * @method array flush(int $limit) = ['status' => 'OK', 'affected_rows' => 284]
 * @method info() = [
 *     'connections' => 27,
 *     'uptime_seconds' => '3675',
 * ]
 */
class PersonStorage
{
    /** @var $pnrData = ['reservation' => [], 'currentPricing' => ['pricingList' => []]] */
    public $pnrData;
    public $mainPerson = ['name' => 'Vasja', 'age' => 21];
    public $allPersons = [];
    public $asdasd = [
        ['segmentType' => 123, 'flightNumber' => '456', 'paxes' => [1,2,3]],
        ['segmentType' => 123, 'flightNumber' => '457', 'paxes' => [1,2,3]],
        ['segmentType' => 123, 'flightNumber' => '458', 'paxes' => [1,2,3]],
    ];
    public function addPerson($name, $age) {
        $this->allPersons[] = [
            'name' => $name,
            'age' => $age,
        ];
        return ['status' => 'OK', 'spaceLeft' => 192846];
    }
    public static function getFields() {
        return [
            'markup' => 'decimal',
            'name' => 'string',
            'price' => 'decimal',
        ];
    }
}

class MyModuleOptions
{
    public static function get()
    {
        return [
            'dependencies' => ['angular5.3', 'jquery3.0', 'bootstrap2.2'],
            'description' => 'This module is used to demonstrate how inline var phpdoc comment provides assoc type information',
            'license' => 'MIT',
            'version' => '00.00.01',
        ];
    }
}

class StaticInferenceParent
{
    protected static $array = [
        'parentKey' => 'parentVal',
    ];

    protected static function getSchema()
    {
        return [
            'id' => 'int',
            'dt' => 'datetime',
        ];
    }

    public static function getArray(): array
    {
        return static::$array;
    }

    public static function getCompleteSchema()
    {
        return static::getSchema() + ['isComplete' => true];
    }
}

class StaticInferenceChild extends StaticInferenceParent
{
    protected static $array = [
        'subKey' => 'subVal',
    ];

    protected static function getSchema()
    {
        return [
            'recordLocator' => 'str',
            'gds' => 'str',
        ];
    }
}

interface IProcessPntQueueAction
{
    /** @param $optionalData = [
     *     'queueNumber' => 123,
     *     'pcc' => 'KLS2',
     *     'sessionData' => ['id' => 1234, 'gds' => 'apollo', 'token' => 'asd324-cxv5345-fhfgh345']
     * ] */
    public function provideImplementedMethArg($optionalData);
}

class TestCaseUtil
{


}
