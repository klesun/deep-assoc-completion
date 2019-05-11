<?php
interface MyInterface {
    /**
     * @return array = ['key1' => '', 'key2' => 0]
     */
    function getData();
}
class MyClass implements MyInterface {
    public function getData() {
        $fromCache = json_decode($_SESSION[MyClass::class.':data'], true);
        if ($fromCache) {
            return $fromCache;
        } else {
            return [
                'key1' => 123,
            ];
        }
    }
}
