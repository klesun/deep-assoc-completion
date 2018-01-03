<?php
namespace Lib;

use Lib\Utils\ArrayUtil;
use Lib\Utils\Fp;

class UnitTest
{
    public static function provideFpMap()
    {
        $list = [];

        // `Fp::map` is our wrapper to `array_map` and should provide same completion
        $records = Fp::map(function($ptcToken){
            if (preg_match('/^(\d+\.\d+)(-\d+\.\d+|)$/', $ptcToken, $matches)) {
                list($_, $from, $to) = $matches;
                $to = substr($to, 1);
                return [
                    'fieldNumber' => explode('.', $from)[0],
                    'firstNameNumber' => explode('.', $from)[1],
                    'through' => $to ? [
                        'fieldNumber' => explode('.', $to)[0],
                        'firstNameNumber' => explode('.', $to)[1],
                    ] : null,
                ];
            } else {
                return null;
            }
        }, explode('/', '1.1-2.2/3.1/4.0'));

        $records[0][''];
        $list[] = [$records[0], [
            'fieldNumber' => [],
            'firstNameNumber' => [],
            'through' => [
                'fieldNumber' => [],
                'firstNameNumber' => [],
            ],
        ]];
 
        // `Fp::filter` is our wrapper to `array_filter` with different arg order
        $filtered = Fp::filter('is_null', $records);
        $list[] = [$filtered[0], ['fieldNumber' => [], 'firstNameNumber' => [], 'through' => []]];

        // `ArrayUtil::getFirst`
        $first = ArrayUtil::getFirst($records);
        $first[''];
        $list[] = [$first, ['fieldNumber' => [], 'firstNameNumber' => [], 'through' => []]];

        // `ArrayUtil::getLast`
        $last = ArrayUtil::getLast($records);
        $list[] = [$last, ['fieldNumber' => [], 'firstNameNumber' => [], 'through' => []]];

        // `Fp::flatten`
        $milkyHolmes2d = [
            'smartGirls' => [
                ['name' => 'Henriette', 'color' => 'black'],
                ['name' => 'Herc', 'color' => 'green'],
                ['name' => 'Kokoro', 'color' => 'blue'],
            ],
            'funnyGirls' => [
                ['name' => 'Sherl', 'color' => 'pink'],
                ['name' => 'Cordelia', 'color' => 'cyan'],
                ['name' => 'Nero', 'color' => 'yellow'],
            ],
        ];
        $flatHolmes = Fp::flatten($milkyHolmes2d);
        $list[] = [$flatHolmes[0], ['name' => [], 'color' => []]];

        // `Fp::groupBy`
        $getFieldNum = function($ptc){return $ptc['fieldNumber'];};
        $fieldNumberToPtcs = Fp::groupBy($getFieldNum, $records);
        $fieldNumberToPtcs[1][0][''];
        $list[] = [$fieldNumberToPtcs[1][0], ['fieldNumber' => [], 'firstNameNumber' => [], 'through' => []]];

        return $list;
    }

    public function provideDbFetch()
    {
        $list = [];
        $rows = Db::inst()->fetchAll('SELECT id, name, profit FROM teams LIMIT 10;');
        $list[] = [$rows['0'], ['id' => [], 'name' => [], 'profit' => []]];
        return $list;
    }
}