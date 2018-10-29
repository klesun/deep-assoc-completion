<?php
$zhopa = ['ololo' => 123];
$GLOBALS['asd'] = 123;
$pizda = [];

class test{
    public function myMethod($array){
        $GLOBALS['dsa'] = 123;
        global $zhopa;
        $zhopa['dzhigurda'] = 456;
        $pizda['asdas'] = 4234;
        foreach($array as $key => $value){
            $GLOBALS[$key] = $value;
        }
    }
}

$GLOBALS['haruka'] = 'kanata';
