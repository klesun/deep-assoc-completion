<?php
namespace Lib\Utils;

class ArrayUtil
{
    public static function overwrite(array $originalArray, array $newArray)
    {
        foreach ($newArray as $key => $value) {
            if (array_key_exists($key, $originalArray)) {
                $originalArray[$key] = $newArray[$key];
            }
        }
        return $originalArray;
    }

    public static function getFirst(array $arr)
    {
        return array_shift($arr);
    }

    public static function getLast(array $arr)
    {
        return array_pop($arr);
    }
}
