<?php

class SomeCls123
{
    /**
     * @var $data = (object) [
     *     'database' => (object)[
     *         'server' => '',
     *         'user' => '',
     *         'pass' => '',
     *         'database' => '',
     *         'port' => '5719',
     *         'socket' => '',
     *     ],
     *     'max_occurences' => 10,
     * ]
     */
    public static $data;

    /**
     * @param $key
     *
     * @return self::$data
     * @throws \Exception
     */
    public function __get($key)
    {
        if (isset(self::$data->$key)) {
            return self::$data->$key;
        }
        throw new \RuntimeException('Configuration property ' . $key . ' not exists.', 2);
    }
}
