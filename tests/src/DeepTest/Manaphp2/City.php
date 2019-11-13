<?php
namespace Manaphp2;

/**
 * @template M
 */
class Query {
    /** @return M */
    public function getModel() {}

    /** @param $filters = get_object_vars(new M) */
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
