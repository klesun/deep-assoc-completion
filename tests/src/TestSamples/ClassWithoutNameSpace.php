<?php


/** @param $employee = [
 *     'salary' => 800.00,
 *     'position' => 'frontend developer',
 *     'fullName' => 'Pupkin Vasja',
 * ] */
function veryveryverylongprefixpromote($employee)
{
    $employee['']; // should suggest: "salary", "position", "fullName"
    return $employee;
}

/**
 * looks like completion from docs does not work
 * in a class without namespace for some reason
 */
class ClassWithoutNameSpace
{
    /** @param $employee = [
     *     'salary' => 800.00,
     *     'position' => 'frontend developer',
     *     'fullName' => 'Pupkin Vasja',
     * ] */
    function promote($employee)
    {
        $employee['']; // should suggest: "salary", "position", "fullName"
        return $employee;
    }
}
