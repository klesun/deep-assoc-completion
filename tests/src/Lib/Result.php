<?php
namespace Lib;

/**
 * @template T
 *
 * Basically the same thing as Rust's std::Result is, only using PHP Exceptions
 */
class Result
{
    public $isOk;
    /** @var T $result */
    public $result;
    public $error;

    /**
     * @param T $result
     */
    public static function makeOk($result)
    {
        return new self(true, $result);
    }

    public static function makeError(\Exception $error)
    {
        return new self(false, null, $error);
    }

    /**
     * @param T $result
     */
    private function __construct($isOk, $result, \Exception $error = null)
    {
        if (!$isOk && is_null($error)) {
            $error = new \Exception('Result Error');
        }

        $this->isOk = $isOk;
        $this->result = $result;
        $this->error = $error;
    }

    public function isOk()
    {
        return $this->isOk;
    }

    /**
     * @return T
     */
    public function unwrap()
    {
        if ($this->isOk()) {
            return $this->result;
        } else {
            throw $this->error;
        }
    }

    /**
     * @param T $default
     * @return
     */
    public function getUsingDefault($default = null)
    {
        if ($this->isOk()) {
            return $this->result;
        } else {
            return $default;
        }
    }

    /** @return \Exception */
    public function getErrorUsingDefault($default = null)
    {
        if ($this->isOk()) {
            return $default;
        } else {
            return $this->error;
        }
    }

    /**
     * @param \Closure $mapper - returns new Result
     * @return Result - same if was error or mapped if ok
     */
    public function flatMap(\Closure $mapper)
    {
        return $this->isOk() ? $mapper($this->unwrap()) : Result::makeError($this->error);
    }

    /**
     * @param callable<T, T_NEW> $mapper
     * @return Result<T_NEW>
     */
    public function map(\Closure $mapper)
    {
        return $this->isOk()
            ? Result::makeOk($mapper($this->unwrap()))
            : Result::makeError($this->error);
    }

    /**
     * @param callable<T, boolean> $pred
     * @return Result<T>
     */
    public function filter(\Closure $pred)
    {
        return $this->isOk() && $pred($this->unwrap())
            ? $this
            : Result::makeError($this->error);
    }

    public function __toString()
    {
        if ($this->isOk()) {
            return 'Result<Ok:'.strval($this->result).'>';
        } else {
            return 'Result<Error>';
        }
    }
}