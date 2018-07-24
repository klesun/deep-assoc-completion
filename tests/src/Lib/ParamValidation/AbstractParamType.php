<?php
namespace Lib\ParamValidation;

use Lib\ParamValidation\IParamType;

/**
 * need this to include "optional/mandatory" shape key
 * logic into constructor to make rule definition shorter
 */
abstract class AbstractParamType implements IParamType
{
    public $optional;

    /**
     * @param string[] $flags used to mark this type as either optional or not
     */
    public function __construct(array $flags = [])
    {
        $this->optional = in_array('optional', $flags);
    }
}
