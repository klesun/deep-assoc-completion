<?php
namespace LocalLib\Lexer;

use Lib\Utils\Fp;

class Lexeme
{
    private $regex;
    private $name;
    private $constraints = [];
    /** @var callable|null */
    private $dataPreprocessor = null;

    public function __construct($name, $regex)
    {
        $this->regex = $regex;
        $this->name = $name;
        //$this->preprocessDataRemoveNumericKeys();
        //$this->preprocessDataReturnOnlyToken();
        $this->preprocessDataReturnDefault();
    }

    private function passesConstraints($context)
    {
        $passesConstraint = function($constraint) use ($context) {return $constraint($context);};
        return Fp::all($passesConstraint, $this->constraints);
    }

    public function hasConstraint(callable $constraint)
    {
        $this->constraints[] = $constraint;
        return $this;
    }

    public function hasPreviousLexemeConstraint($lexemes)
    {
        $constraint = function($context) use ($lexemes) {
            $previousLexeme = array_pop($context['lexemes']);
            return in_array($previousLexeme['lexeme'], $lexemes);
        };
        return $this->hasConstraint($constraint);
    }

    public function preprocessData(callable $dataPreprocessor)
    {
        $this->dataPreprocessor = $dataPreprocessor;
        return $this;
    }

    public function preprocessDataFilterTokens($tokens)
    {
        $dataPreprocessor = function($data) use ($tokens) {
            $result = [];
            foreach ($tokens as $token) {
                $result[$token] = $data[$token] ?? null;
            }
            return $result;
        };
        return $this->preprocessData($dataPreprocessor);
    }

    public function preprocessDataEmpty()
    {
        $dataPreprocessor = function($data){return $data;};
        return $this->preprocessData($dataPreprocessor);
    }

    public function preprocessDataRemoveNumericKeys()
    {
        $dataPreprocessor = function($data){
            $result = [];
            foreach ($data as $key => $value) {
                if (!is_integer($key)) {
                    $result[$key] = $value;
                }
            }
            return $result;
        };
        return $this->preprocessData($dataPreprocessor);
    }

    public function preprocessDataReturnOnlyToken()
    {
        $dataPreprocessor = function($data) {
            $result = [];
            foreach ($data as $key => $value) {
                if (!is_integer($key)) {
                    $result[$key] = $value;
                }
            }

            if (count($result) === 1) {
                return array_pop($result);
            } else {
                return null;
            }
        };
        return $this->preprocessData($dataPreprocessor);
    }

    public function preprocessDataReturnDefault()
    {
        $dataPreprocessor = function($data) {
            $result = [];
            foreach ($data as $key => $value) {
                if (!is_integer($key)) {
                    $result[$key] = $value;
                }
            }

            if (count($result) === 1) {
                return array_pop($result);
            } elseif (count($result) > 1) {
                return $result;
            } else {
                return null;
            }
        };
        return $this->preprocessData($dataPreprocessor);
    }

    public function match($text, $context = null)
    {
        $dataPreprocessor = $this->dataPreprocessor;
        if (preg_match($this->regex, $text, $matches) && $this->passesConstraints($context) && $matches[0] !== '') {
            return [
                'lexeme'   => $this->name,
                'data'     => $dataPreprocessor($matches),
                'textLeft' => mb_substr($text, mb_strlen($matches[0])),
            ];
        } else {
            return null;
        }
    }
}
