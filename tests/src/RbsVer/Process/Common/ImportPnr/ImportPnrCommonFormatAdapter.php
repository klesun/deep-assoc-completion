<?php
namespace RbsVer\Process\Common\ImportPnr;

class ImportPnrCommonFormatAdapter
{
    /** @param $reservation = IGdsPnrFieldsProvider::getReservation() */
    public static function addContextDataToPaxes($reservation)
    {
        $reservation['passengers'] = array_map(function($pax) {
            $pax['ageGroup'] = 'adult';
            return $pax;
        }, $reservation['passengers']);
        return $reservation;
    }
}
