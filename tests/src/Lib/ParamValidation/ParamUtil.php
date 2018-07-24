<?php
namespace Lib\ParamValidation;

use Lib\ParamValidation\BoolP;
use Lib\ParamValidation\DictP;
use Lib\ParamValidation\IntP;
use Lib\ParamValidation\ListP;
use Lib\ParamValidation\OneOfP;
use Lib\ParamValidation\StringP;

/**
 * takes a Param Validation scheme and returns sample data
 * should be extremely useful for completion from Type Hints
 * could also be useful to make boilerplate params when doing usage examples for docs
 */
class ParamUtil
{
    public static function sample($scheme)
    {
        $sampleData = [];
        if ($scheme instanceof DictP) {
            foreach ($scheme->definition as $key => $value) {
                $sampleData[$key] = static::sample($value);
            }
        } elseif ($scheme instanceof ListP) {
            $sampleData[] = static::sample($scheme->elemType);
        } elseif ($scheme instanceof OneOfP) {
            if ($opts = $scheme->options) {
                $i = rand(0, count($opts) - 1);
                $sampleData = static::sample($opts[$i]);
            } else {
                $sampleData = null;
            }
        } elseif ($scheme instanceof StringP) {
            if ($oneOf = $scheme->oneOf) {
                return array_pop($oneOf);
            } else {
                $sampleData = 'sample';
            }
        } elseif ($scheme instanceof IntP) {
            $sampleData = -100;
        } elseif ($scheme instanceof BoolP) {
            $sampleData = false;
        } else {
            $sampleData = null;
        }
        return $sampleData;
    }
}
