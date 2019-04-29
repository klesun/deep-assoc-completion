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

    /**
     * @param string $field =array_keys(static::sample())[$i]
     *
     * @return bool
     */
    public function hasField($field)
    {
    }
}

class Child extends Base
{
    public static function sample()
    {
        return ['user_id' => 1, 'user_name' => 'mark'];
    }
}

Child::find([], ['']);
(new Child)->hasField('');
