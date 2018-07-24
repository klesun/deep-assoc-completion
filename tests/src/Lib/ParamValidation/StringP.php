<?php
namespace Lib\ParamValidation;

use Lib\Result;
use Lib\ParamValidation\AbstractParamType;
use Lib\ParamValidation\ErrorStructure;
use Lib\Utils\Fp;

class StringP extends AbstractParamType
{
    private $minLen;
    private $maxLen;
    /** @var string - regex pattern, same string you would pass to preg_match */
    private $pattern;
    /** @var string[] */
    private $oneOf;
    /** @var \Closure that takes string and returns error list */
    private $format;
    /** @var string - overrides all error messages if defined */
    private $errMsg;

    public function __construct(array $flags, array $options)
    {
        parent::__construct($flags);

        $this->minLen = $options['minLen'] ?? null;
        $this->maxLen = $options['maxLen'] ?? null;
        $this->pattern = $options['pattern'] ?? null;
        $this->oneOf = $options['oneOf'] ?? null;
        $is = $options['is'] ?? null;
        if (!is_null($is)) {
            $this->oneOf = [$is];
        }
        $this->format = $options['format'] ?? null;
        $this->errMsg = $options['errMsg'] ?? null;

        // preventing developer mistakes
        if ($this->maxLen !== null && $this->minLen !== null && $this->maxLen < $this->minLen) {
            throw new \Exception('StringP $options[\'minLen\'] may not be greater than $options[\'maxLen\']');
        }
    }

    public function validate($value): ErrorStructure
    {
        if (is_array($value) || is_object($value)) {
            return new ErrorStructure(['must be a string, but you provided ['.gettype($value).']']);
        }

        $violatedConditions = [];

        if ($this->minLen !== null &&
            mb_strlen($value) < $this->minLen
        ) {
            $violatedConditions[] = $this->minLen === 1
                ? 'non empty'
                : '>= '.$this->minLen.' characters long';
        }
        if ($this->maxLen !== null &&
            mb_strlen($value) > $this->maxLen
        ) {
            $violatedConditions[] = '<= '.$this->maxLen.' characters long';
        }
        if ($this->pattern !== null &&
            !preg_match($this->pattern, $value)
        ) {
            $violatedConditions[] = 'matching '.$this->pattern.' regex';
        }
        if ($this->oneOf !== null &&
            !in_array($value, $this->oneOf)
        ) {
            $quoted = array_map('self::quote', $this->oneOf);
            $violatedConditions[] = 'one of ['.implode(', ', $quoted).']';
        }
        if ($this->format !== null &&
            $errorMsgs = $this->format->__invoke($value)
        ) {
            $violatedConditions = array_merge($violatedConditions, $errorMsgs);
        }

        return count($violatedConditions) > 0
            ? ($this->errMsg === null
                ? new ErrorStructure(['must be '.implode(', ', $violatedConditions).' string'])
                : new ErrorStructure([$this->errMsg]))
            : ErrorStructure::noErrors();
    }

    /** @return \Closure that takes string and returns
     * violated condition list in case of format mismatch */
    public static function getMysqlDatetimeFormat(): \Closure
    {
        return function(string $value) {
            $pattern = '/^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}(:\d{2})?(.\d+)?)?$/';
            return preg_match($pattern, $value) ? [] : [
                'matching MySQL datetime format (Y-m-d H:i:s)',
            ];
        };
    }

    public static function getDecimalFormat($params = []): \Closure
    {
        return function(string $value) use ($params): array {
            $violatedConditions = [];
            if (preg_match('/^\d+(\.\d+)?$/', $value)) {
                $min = $params['min'] ?? null;
                if ($min !== null && floatval($value) < $min) {
                    $violatedConditions[] = '>= '.$min;
                }
                $max = $params['max'] ?? null;
                if ($max !== null && floatval($value) > $max) {
                    $violatedConditions[] = '<= '.$max;
                }
            } else {
                $violatedConditions[] = 'decimal';
            }

            return $violatedConditions;
        };
    }

    private static function quote(string $str)
    {
        return '\''.addcslashes($str, '\'\\').'\'';
    }
}
