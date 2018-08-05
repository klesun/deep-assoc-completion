<?php
namespace Lib;

use Lib\ParamValidation\DictP;
use Lib\ParamValidation\ListP;
use Lib\ParamValidation\ParamUtil;
use Lib\ParamValidation\StringP;
use Lib\Utils\ArrayUtil;
use Lib\Utils\Fp;
use Lib\Utils\MemoizedFunctions;
use LocalLib\Lexer\Lexeme;
use LocalLib\Lexer\Lexer;

class UnitTest
{
    private static function makeNameRecords()
    {
        return Fp::map(function($ptcToken){
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
    }

    public static function provideFpMap()
    {
        $list = [];

        // `Fp::map` is our wrapper to `array_map` and should provide same completion
        $records = static::makeNameRecords();

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

        return $list;
    }

    public static function provideFpMap2()
    {
        $list = [];

        // `Fp::map` is our wrapper to `array_map` and should provide same completion
        $records = static::makeNameRecords();

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

    /** @param $row = Db::fetchAll('SELECT * FROM delete_me')[0]; */
    public function provideDbFetchInADoc($row)
    {
        $row['name'];
        $list = [];
        $rows = Db::inst()->fetchAll('SELECT id, name, profit FROM teams LIMIT 10;');
        $list[] = [$rows['0'], ['id' => [], 'name' => [], 'profit' => []]];
        return $list;
    }

    public function provideZip()
    {
        $list = [];
        $grooms = [
            ['smoking' => 'black', 'salary' => '510090.00'],
            ['smoking' => 'red', 'salary' => '710090.00'],
        ];
        $wives = [
            ['weddingDress' => 'expensiveOne', 'face' => 'average'],
            ['weddingDress' => 'cheapOne', 'face' => 'beautiful'],
        ];
        $weddingPairs = Fp::zip([$grooms, $wives]);
        $firstPair = $weddingPairs[0];
        $list[] = [$firstPair[0], ['smoking' => [], 'salary' => []]];
        $list[] = [$firstPair[1], ['weddingDress' => [], 'face' => []]];
        return $list;
    }

    private static function pairsToDict(array $pairs)
    {
        $result = [];
        foreach ($pairs as list($key, $value)) {
            $result[$key] = $value;
        }
        return $result;
    }

    public function providePairsToDict()
    {
        $list = [];
        $pairs = [
            ['gds', 'apollo'],
            ['profile', 'GENERIC'],
            ['user', 'RBS'],
        ];
        $dict = static::pairsToDict($pairs);
        $list[] = [$dict, ['gds' => [], 'profile' => [], 'user' => []]];
        return $list;
    }

    private static function unpackList(array $keys, array $data)
    {
        $result = [];
        foreach ($data as $record) {
            $pairs = Fp::zip([$keys, $record]);
            $result[] = static::pairsToDict($pairs);
        }
        return $result;
    }

    public static function provideTableOwnImplementation()
    {
        $list = [];
        $profiles = [
            // gds       | profile                | user   | group                 | gds_profile                | session_limit | idle_session_timeout |
            //-----------+------------------------+--------+-----------------------+----------------------------+---------------+----------------------|
            [  'amadeus' , 'GENERIC'              , 'RBS'  ,  null                 , 'AMADEUS_PROD_1ASIWTUTICO' , null          , null                 ],
            [  'apollo'  , 'AIRLINE_TERMINAL'     , 'AFPE' , 'apollo_terminals'    , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'CMS_TERMINAL'         , 'RBS'  ,  null                 , 'DynApolloProd_2G55'       , 1000          , null                 ],
            [  'apollo'  , 'FPE_STUDENT_TERMINAL' , 'FPE'  , 'apollo_terminals'    , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'GRAB_FARES'           , 'RBS'  , 'apollo_auto_process' , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'IMPORT_PNR'           , 'RBS'  , 'apollo_auto_process' , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'QUEUE_PROCESSING'     , 'RBS'  , 'apollo_auto_process' , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'UNCLASSIFIED'         , 'RBS'  , 'apollo_auto_process' , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'OTA'                  , 'OTA'  , 'apollo_auto_process' , 'DynApolloProd_1O3K'       , null          , null                 ],
            [  'apollo'  , 'STAGING'              , 'RBS'  ,  null                 , 'DynApolloCopy_1O3K'       , 200           , null                 ],
            [  'sabre'   , 'CMS_OLD_TERMINAL'     , 'FPE'  , 'sabre_terminals'     , 'SABRE_PROD_L3II'          , null          , 5*60                 ],
            [  'sabre'   , 'CMS_TERMINAL'         , 'RBS'  , 'sabre_terminals'     , 'SABRE_PROD_L3II'          , null          , null                 ],
            [  'sabre'   , 'FPE_STUDENT_TERMINAL' , 'FPE'  , 'sabre_terminals'     , 'SABRE_PROD_Z2NI'          , null          , 5*60                 ],
            [  'sabre'   , 'IMPORT_PNR'           , 'RBS'  , 'sabre_auto_process'  , 'SABRE_PROD_L3II'          , null          , null                 ],
            [  'sabre'   , 'QUEUE_PROCESSING'     , 'RBS'  , 'sabre_auto_process'  , 'SABRE_PROD_L3II'          , null          , null                 ],
            [  'sabre'   , 'OTA'                  , 'OTA'  , 'sabre_auto_process'  , 'SABRE_PROD_L3II'          , null          , null                 ],
            [  'sabre'   , 'UNCLASSIFIED'         , 'RBS'  , 'sabre_auto_process'  , 'SABRE_PROD_L3II'          , null          , null                 ],
        ];
        $unpacked = static::unpackList(['gds', 'profile', 'user', 'group', 'gds_profile', 'session_limit', 'idle_session_timeout'], $profiles);
        $row = $unpacked[0];
        $list[] = [$row, ['gds' => [], 'profile' => [], 'user' => [], 'group' => [], 'gds_profile' => [], 'session_limit' => [], 'idle_session_timeout' => []]];
        return $list;
    }

    public static function fetchTdData(string $ticketDesignator)
    {
        return [
            'is_published' => true,
            'prefix' => substr($ticketDesignator, 0, 3),
            'correct_cmd' => '$B:N',
        ];
    }

    public static function provideValPassedToObjCtor()
    {
        $result = Result::makeError(new \Exception('asd'));
        if (rand() % 2) {
            $result = Result::makeOk(['a' => 5, 'b' => 6]);
        } elseif (rand() % 3) {
            $result = Result::makeOk(['e' => 5, 'f' => 6]);
        }
        $result->result[''];
        $list[] = [$result->result, ['a' => [], 'b' => [], 'e' => [], 'f' => []]];
        $unwrapped = $result->unwrap();
        $unwrapped[''];
        $list[] = [$unwrapped, ['a' => [], 'b' => [], 'e' => [], 'f' => []]];
        return $list;
    }

    public function provideOptionChain()
    {
        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})->unwrap()[''];

        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})
            ->filter(function($value4){return $value4[''];});

        $result = \Lib\Result::makeOk(['a' => 1])
            ->map(function($value1){return $value1 + ['b' => 2];})
            ->map(function($value2){return $value2 + ['c' => 3];})
            ->map(function($value3){return $value3 + ['d' => 4];})
            ->filter(function($value4){return $value4[''];})
            ->map(function($value4){return $value4 + ['e' => 5];})
        ;
        $result->result[''];
        $result->unwrap()[''];
        $list[] = [$result->unwrap(), ['a' => [], 'b' => [], 'c' => [], 'd' => [], 'e' => []]];
        return $list;
    }

    public static function provideInstRecursion()
    {
        $schema = new DictP([], [
            'gds' => new StringP([], []),
            'recordLocator' => new StringP([], []),
            'passengers' => new ListP([], ['elemType' => new DictP([], [
                'absoluteNumber' => new StringP([], []),
                'fullName' => new StringP([], []),
            ])]),
            'segments' => new ListP([], ['elemType' => new DictP([], [
                'airline' => new StringP([], []),
                'departureAirport' => new StringP([], []),
            ])]),
        ]);
        $sample = ParamUtil::sample($schema);
        $sample['passengers'][0][''];
        // must not reach the 10000 expression limit
        $list[] = [$sample['passengers'][0], ['absoluteNumber' => [], 'fullName' => []]];
        $list[] = [$sample['segments'][0], ['airline' => [], 'departureAirport' => []]];
        return $list;
    }

    public function provideLexer(string $modsPart)
    {
        $onlyRaw = function($matches){return ['raw' => $matches[0], 'parsed' => null];};
        $lexer = new Lexer([
            (new Lexeme('departureDate', '/^¥V(\d{1,2}[A-Z]{3}\d{0,2})/'))->preprocessData($onlyRaw),
            (new Lexeme('returnDate', '/^¥R(\d{1,2}[A-Z]{3}\d{0,2})/'))->preprocessData($onlyRaw),
        ]);
        // $this->context used as a local variable - we can support that
        $lexed = $lexer->lex($modsPart);
        $lexed['lexemes'][0][''];
        $lexed['lexemes'][0]['lexeme'] === '';
        $typeToData = array_combine(
            array_column($lexed['lexemes'], 'lexeme'),
            array_column($lexed['lexemes'], 'data')
        );
        $asd = explode(' ', 'sad asd sada');
        $asd[0];
        $typeToData[''];
        $list[] = [$typeToData, ['departureDate' => [], 'returnDate' => []]];
        return $list;
    }

    public static function provideCachedFuncCallSimple()
    {
        $list = [];
        $args = ['CHD054'];
        $tdData1 = MemoizedFunctions::ramCachedFunctionCall(
            'asd', [static::class, 'fetchTdData'], $args
        );
        $tdData1[''];
        $list[] = [$tdData1, ['is_published' => [], 'prefix' => [], 'correct_cmd' => []]];

        $tdData2 = MemoizedFunctions::cachedFunctionCall(
            'asd', [static::class, 'fetchTdData'], $args, 60*60
        );
        $tdData2[''];
        $list[] = [$tdData2, ['is_published' => [], 'prefix' => [], 'correct_cmd' => []]];
        return $list;
    }

    /**
     * following not resolved yet
     */

    public static function provideCachedFuncCall()
    {
        $list = [];
        $args = ['CHD054'];
        $tdData3 = MemoizedFunctions::cachedBothWays(
            [static::class, 'fetchTdData'], $args, 60*60
        );
        $tdData3[''];
//        $list[] = [$tdData3, ['is_published' => [], 'prefix' => [], 'correct_cmd' => []]];

        return $list;
    }
}
