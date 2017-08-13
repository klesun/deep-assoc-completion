<?php
namespace Lib\Utils;

// I guess it really should be replaced by Func library and custom functions
class Fp
{
    public static function map(callable $function, array $arr)
    {
        return array_map($function, $arr);
    }

    /**
     * $args is array of arrays (using it instead of func_get_args, beacause making and array is simpler than call_user_func_array)
     * Behaves like Python's zip (zips, cutting arrays if ones are longer than others)
     */
    public static function zip(array $args)
    {
        $zipped = [];
        $n = count($args);
        for ($i=0; $i<$n; ++$i) {
            reset($args[$i]);
        }
        while ($n) {
            $tmp = [];
            for ($i=0; $i<$n; ++$i) {
                if (key($args[$i]) === null) {
                    break 2;
                }
                $tmp[] = current($args[$i]);
                next($args[$i]);
            }
            $zipped[] = $tmp;
        }
        return $zipped;
    }

    public static function all(callable $function, array $arr)
    {
        foreach ($arr as $el) {
            if (!call_user_func($function, $el)) {
                return false;
            }
        }
        return true;
    }

    public static function filter(callable $function, array $arr)
    {
        return array_filter($arr, $function);
    }

    public static function any(callable $function, array $arr)
    {
        foreach ($arr as $el) {
            if (call_user_func($function, $el)) {
                return true;
            }
        }
        return false;
    }

    public static function selectKeysFromArray(array $arr, array $keys)
    {
        $result = [];
        foreach ($keys as $key) {
            if (array_key_exists($key, $arr)) {
                $result[$key] = $arr[$key];
            } else {
                $result[$key] = null;
            }
        }
        return $result;
    }

    /**
     * Sort array $arr using result of $getValueToSortBy as the value to sort by.
     */
    public static function sortBy(callable $getValueToSortBy, array $arr, $reverse = false)
    {
        $sortable = [];
        foreach ($arr as $key => $el) {
            $sortable[$key] = $getValueToSortBy($el);
        }
        $order = $reverse ? SORT_DESC : SORT_ASC;
        array_multisort($sortable, $order, $arr);
        return $arr;
    }

    public static function flatten($iterables)
    {
        $res = [];
        foreach ($iterables as $iterable) {
            foreach ($iterable as $value) {
                $res[] = $value;
            }
        }
        return $res;
    }

    public static function unique(array $array)
    {
        return array_unique($array);
    }

    public static function reverse(array $array)
    {
        return array_reverse($array);
    }

    /*
     * groupBy or factor([1,2,3,4,5,6,7], function(x){if (x % 2 === 0) {return 'even';} else {return 'odd';}})  -->
     * [
     *   'even' => [2,4,6]
     *   'odd'  => [1,3,5,7]
     * ]
     */
    public static function groupBy(callable $function, $iterable)
    {
        $result = [];
        foreach ($iterable as $value) {
            $factorGroup = $function($value);
            if (array_key_exists($factorGroup, $result)) {
                $result[$factorGroup][] = $value;
            } else {
                $result[$factorGroup] = [$value];
            }
        }
        return $result;
    }

    /**
     * wraps $value into an array if $value is not array
     */
    public static function ensureArray($value): array
    {
        return is_array($value) ? $value : [$value];
    }

    public static function minBy(array $arr, \Closure $f)
    {
        $minVal = null;
        $minCost = null;
        foreach ($arr as $value) {
            $cost = $f($value);
            if (is_null($minCost) || $minCost > $cost) {
                $minCost = $cost;
                $minVal = $value;
            }
        }
        return $minVal;
    }

    public static function maxBy(array $arr, \Closure $f)
    {
        $maxVal = null;
        $maxCost = null;
        foreach ($arr as $value) {
            $cost = $f($value);
            if (is_null($maxCost) || $maxCost < $cost) {
                $maxCost = $cost;
                $maxVal = $value;
            }
        }
        return $maxVal;
    }

    public static function chunk(array $arr, int $n)
    {
        $result = [];
        while (true) {
            $chunk = [];
            for ($i = 0; $i < $n; $i++) {
                if (!$arr) {
                    if ($chunk) {
                        $result[] = $chunk;
                        $chunk = [];
                    }
                    break 2;
                } else {
                    $chunk[] = array_shift($arr);
                }
            }
            $result[] = $chunk;
        }
        return $result;
    }

    public static function sublists(array $arr, int $n)
    {
        $arr = array_values($arr);
        $result = [];
        for ($i = 0; $i < count($arr) - $n + 1; $i++) {
            $chunk = [];
            for ($j = 0; $j < $n; $j++) {
                $chunk[] = $arr[$i + $j];
            }
            $result[] = $chunk;

        }
        return $result;
    }
}
