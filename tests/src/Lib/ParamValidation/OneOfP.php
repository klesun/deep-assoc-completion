<?php
namespace Lib\ParamValidation;

use Lib\Utils\Fp;

/**
 * used to express parameters that can have either one or another format, example:
 * $commissionSchema = new OneOf([], [
 *     new DictP([], [
 *         'units' => new StringP([], ['is' => 'percent']),
 *         'value' => new IntP([]),
 *     ]),
 *     new DictP([], [
 *         'units' => new StringP([], ['is' => 'amount']),
 *         'value' => new StringP([], ['pattern' => '/^\d+\.\d+$/']),
 *     ]),
 * ])
 */
class OneOfP extends AbstractParamType
{
    private $options;

    /** @param AbstractParamType[] $options */
    public function __construct(array $flags, array $options)
    {
        parent::__construct($flags);
        $this->options = $options;
    }

    public function validate($value): ErrorStructure
    {
        $errorBundles = [];
        foreach ($this->options as $option) {
            $errors = $option->validate($value);
            if ($errors->isEmpty()) {
                return ErrorStructure::noErrors();
            } else {
                $errorBundles[] = $errors;
            }
        }
        if ($errorBundles) {
            $ownErrors = Fp::map(function($errors){
                return 'either:'.PHP_EOL.implode(PHP_EOL, $errors->getErrors());
            }, $errorBundles);
            return (new ErrorStructure($ownErrors));
        } else {
            return (new ErrorStructure(['No format options specified in Param Validation']));
        }
    }
}
