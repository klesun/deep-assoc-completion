<?php

/**
 * reproduces only when file is located in the root
 * or when there are not much files in the project
 */
class DumbServiceContractViolationExample
{
    public function getFunctions()
    {
        // if var::multiResolve() is used in Type Provider, this
        // case triggers Contract Violation runtime error
        $asd = function($variables, $value) {
            return $variables['hujtainer']->get($value);
        };
    }
}
