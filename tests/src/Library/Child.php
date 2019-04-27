<?php
namespace Library;

class Base
{

    /**
     * @return array
     */
    public static function sample()
    {
        return ['stub_id' => 1];
    }

    /**
     * @param array $filters
     * @param array $fields =static::sample()
     *
     * @return array
     */
    public static function find($filters, $fields = null)
    {
        return [];
    }
}

class Child extends Base
{
    public static function sample()
    {
        return ['user_id' => 1, 'user_name' => 'mark'];
    }
}
