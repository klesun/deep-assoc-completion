<?php
namespace Manaphp2;

/**
 * @template M
 */
class Query {
    private static function addMoreKeys($model) {
        return array_merge(get_object_vars($model), ['ololo' => 123]);
    }

    /** @return M */
    public function getModel() {}

    /** @param $filters = Query::addMoreKeys(new M) */
    public function where($filters) {}
}

abstract class Model {
    /** @return \Manaphp2\Query<static> */
    public static function where($filters) {}
}

class City extends \Manaphp2\Model {
    public $iataCode;
    public $countryCode;
}

City::where()->getModel()->ia;
City::where()->where(['' => '']);
