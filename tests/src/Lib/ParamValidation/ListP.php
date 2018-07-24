<?php
namespace Lib\ParamValidation;

use Lib\ParamValidation\AbstractParamType;
use Lib\ParamValidation\ErrorStructure;
use Lib\ParamValidation\IParamType;

/**
 * instances of this class can validate some
 * value to be a list that matches passed rules
 */
class ListP extends AbstractParamType
{
    /** @var IParamType */
    private $elemType;
    /** @var int */
    private $minLen;
    /** @var int */
    private $maxLen;

    private static function popKey(array &$dict, string $key, $defaultValue = null)
    {
        $value = $dict[$key] ?? $defaultValue;
        unset($dict[$key]);
        return $value;
    }

    public function __construct(array $flags, array $options)
    {
        parent::__construct($flags);

        $this->minLen = self::popKey($options, 'minLen');
        $this->maxLen = self::popKey($options, 'maxLen');
        $this->elemType = self::popKey($options, 'elemType');

        // preventing developer mistakes
        if (count($options) > 0) {
            throw new \Exception('ListP $options contains unknown keys: '.implode(', ', array_keys($options)));
        }
        if ($this->elemType && !$this->elemType instanceof IParamType) {
            throw new \Exception('ListP $options[\'elemType\'] must implement IParamType interface');
        }
        if ($this->maxLen !== null && $this->minLen !== null && $this->maxLen < $this->minLen) {
            throw new \Exception('ListP $options[\'minLen\'] may not be greater than $options[\'maxLen\']');
        }
    }

    private static function isSequential(array $array)
    {
        $orders = count($array) > 0 ? range(0, count($array) - 1) : [];
        return array_keys($array) == $orders;
    }

    public function validate($value): ErrorStructure
    {
        if (!is_array($value)) {
            return new ErrorStructure(['must be an array, but you provided ['.gettype($value).']']);
        } elseif (!static::isSequential($value)) {
            return new ErrorStructure(['must be a _list_ array, but you provided an associative array - it has non-sequential keys']);
        }

        $violatedConditions = [];

        if ($this->minLen !== null &&
            $this->minLen > count($value)
        ) {
            $violatedConditions[] = $this->minLen === 1
                ? 'non-empty'
                : '>= '.$this->minLen.' elements long';
        }
        if ($this->maxLen !== null &&
            $this->maxLen < count($value)
        ) {
            $violatedConditions[] = '<= '.$this->maxLen.' elements long';
        }

        $errors = count($violatedConditions) > 0
            ? new ErrorStructure(['must be '.implode(', is ', $violatedConditions).' array'])
            : ErrorStructure::noErrors();

        if ($this->elemType !== null) {
            foreach ($value as $idx => $elem) {
                $elemErrors = $this->elemType->validate($elem);

                if (!$elemErrors->isEmpty()) {
                    $errors->children[$idx] = $elemErrors;
                }
            }
        }

        if (count($errors->children) > 5 &&
            count($errors->children) === count($value)
        ) {
            // if all list elements have wrong format i believe
            // no need to repeat same errors thousand times

            $firstChildErrors = $errors->children[0];
            $errors->children = [
                '*' => $firstChildErrors,
            ];
        }

        return $errors;
    }
}
