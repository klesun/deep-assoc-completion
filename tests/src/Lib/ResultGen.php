<?php
namespace Lib;

/**
 * @template T
 *
 * Basically the same thing as Rust's std::Result is, only using PHP Exceptions
 */
class ResultGen
{
    public $isOk;
    /** @var T $result */
    public $result;
    public $error;

    /** @param T $result */
    public function __construct($isOk, $result, \Exception $error = null) {}

    /**
     * @param T $result
     * @return ResultGen<T>
     */
    public static function makeOk($result) {}

    /** @return ResultGen */
    public static function makeError(\Exception $error) {}

    /** @return bool */
    public function isOk() {}

    /** @return T */
    public function unwrap() {}

    /**
     * @param T $default
     * @return T
     */
    public function getUsingDefault($default = null) {}

    /** @return \Exception */
    public function getErrorUsingDefault($default = null) {}

    /**
     * @template Tnew
     * @param callable<T, ResultGen<Tnew>> $mapper - returns new Result
     * @return ResultGen<Tnew>
     */
    public function flatMap(\Closure $mapper) {}

    /**
     * @param callable<T, T_NEW> $mapper
     * @return ResultGen<T_NEW>
     */
    public function map(\Closure $mapper) {}

    /**
     * @param callable<T, bool> $pred
     * @return ResultGen<T>
     */
    public function filter(\Closure $pred) {}
}