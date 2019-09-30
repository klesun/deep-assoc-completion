<?php

class C
{
    public function D()
    {
        echo 'Hello, world!';
    }
}

class A
{
    public $B = [];

    public function __construct()
    {
        $this->B['C'] = new C;
    }
}

$A = new A;
$A->B['C']->D();
