<?php
class UsageCircularReferencesGetExample
{
    private static function subSubSubStrackOVerflow($value)
    {
        return [
            'value' => self::provideStackOverflow($value),
            'ololo' => 12313,
            'lalala' => 54645645,
        ];
    }

    private static function subSubStrackOVerflow($value)
    {
        return [
            'value' => self::subSubSubStrackOVerflow($value),
            'ololo' => 12313,
            'lalala' => 54645645,
        ];
    }

    private static function subStrackOVerflow($value)
    {
        return [
            'value' => self::subSubStrackOVerflow($value),
            'hujalue' => self::subStrackOVerflow($value),
            'asdasd' => self::provideStackOverflow($value),
            'ololo' => 12313,
            'lalala' => 54645645,
        ];
    }

    /** @param $zhopa = self::provideStackOverflow() */
    public static function provideStackOverflow($zhopa)
    {
        self::provideStackOverflow([$zhopa]);
        self::provideStackOverflow($zhopa[2]);
        self::provideStackOverflow(['']);
        /** @var $subarr = self::provideStackOverflow() */
//        $subarr = [
//            $zhopa,
//            $zhopa => 123,
//            'asdqwe' => 321,
//        ] + $zhopa;
        /** @var $subarr = self::subStrackOVerflow() */
//        $subarr = [
//            $zhopa,
//            $zhopa => 123,
//            'asdqwe' => 321,
//        ] + $zhopa;
//        /** @var $subarr = self::provideStackOverflow() */
        $subarr = [
            self::subStrackOVerflow($zhopa),
            self::subStrackOVerflow($zhopa) => 123,
            $zhopa => self::subStrackOVerflow($zhopa),
            'asdqwe' => 321,
        ] + $zhopa;
        $zhopa[] = $subarr;
        return $zhopa;
    }

    public function __get($name)
    {
        self::provideStackOverflow($name)[$name];
        $obj = (object)self::provideStackOverflow($name);
        $obj->$name;
    }
}
