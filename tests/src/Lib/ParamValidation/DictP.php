<?php
namespace Lib\ParamValidation;

class DictP
{
    private $definition;

    public function __construct(array $flags, array $definition)
    {
        $this->definition = $definition;
    }
}