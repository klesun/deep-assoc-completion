<?php
class Blanc {
    private $loves = ['Rom', 'Ram'];
    protected $likes = ['Neptune', 'B-Sha'];
    public $hates = ['Vert'];
    public static $dream = 'Big Boobs';

    public function getAngry()
    {
        $this->hates[] = self::beAngry();
    }

    public static function beAngry()
    {
        return 'everyone';
    }
}