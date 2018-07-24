<?php
namespace Lib\ParamValidation;

use Lib\ParamValidation\ErrorStructure;

/**
 * a something you can call validate($value) and get a non empty
 * @see ErrorStructure in case of mismatch
 */
interface IParamType
{
    /**
     * @return ErrorStructure - validation was successful if
     * @see ErrorStructure::isEmpty() is true
     */
    public function validate($value): ErrorStructure;
}
