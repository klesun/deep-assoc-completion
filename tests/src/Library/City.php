<?php
namespace App\Models;

class Model
{
    public function first($table, $params)
    {
    }
}

class City extends Model
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
