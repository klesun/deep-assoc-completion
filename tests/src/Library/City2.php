<?php
namespace App\Models;

class Model
{
    public static function first($table, $params)
    {
    }
}

class City2 extends Model
{
    public $city_id;
    public $city;
    public $country_id;
    public $last_update;

    public function getDisplayField()
    {
        return 'city';
    }
}


class TimeController
{
    public function currentAction()
    {
        return City::first([],['']); //caret
    }
}