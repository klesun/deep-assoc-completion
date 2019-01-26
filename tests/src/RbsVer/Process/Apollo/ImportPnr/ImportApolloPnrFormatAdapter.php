<?php
namespace RbsVer\Process\Apollo\ImportPnr;
use RbsVer\Process\Common\ImportPnr\ImportPnrCommonFormatAdapter;

/**
 * transforms output of the ImportApolloPnrAction to a common for any GDS structure
 */
class ImportApolloPnrFormatAdapter
{
    /** @param $reservation = IGdsPnrFieldsProvider::getReservation() */
    public static function transformReservation($reservation)
    {
        $reservation['passengers'] = [];
        $reservation['passengers'] = array_map('doStuff', $reservation['passengers']);
        return $reservation;
    }
}
