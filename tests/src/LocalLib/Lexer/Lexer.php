<?php
namespace LocalLib\Lexer;

use Lib\Utils\Fp;

class Lexer
{
    private $context;
    private $lexemes;
    private $logger;

    public function __construct($lexemes)
    {
        $this->lexemes = $lexemes;
    }

    public function setLog($log)
    {
        $this->logger = $log;

        return $this;
    }

    public function log($msg, $data = null)
    {
        $log = $this->logger;
        if ($log) {
            $log($msg, $data);
        }
    }

    private function matchLexeme($text)
    {
        foreach ($this->lexemes as $lexeme) {
            $r = $lexeme->match($text, $this->context);
            if ($r) {
                return $r;
            }
        }
        return null;
    }

    public function lex($text)
    {
        $this->context = [
            'text' => $text,
            'lexemes' => [],
        ];

        while(true){
            $lexeme = $this->matchLexeme($this->context['text']);
            if ($lexeme) {
                $this->log('Lexeme: '.$lexeme['lexeme'], $lexeme);
                $this->context['text'] = $lexeme['textLeft'];
                $this->context['lexemes'][] = $lexeme;
            } else {
                $this->log('ERROR: '.$this->context['text']);
                break;
            }
        }

        // Not sure if appropriate
        $removeTextLeft = function($data){unset($data['textLeft']); return $data;};
        $this->context['lexemes'] = Fp::map($removeTextLeft, $this->context['lexemes']);
        return $this->context;
    }
}
