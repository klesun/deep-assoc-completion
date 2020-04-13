<?php
if (!function_exists("array_column")) {
    /**
     * @param $array
     * @param $column_name
     * @return array
     */
    function array_column($array, $column_name)
    {
        return array_map(function ($element) use ($column_name) {
            return $element[$column_name];
        }, $array);
    }
}
