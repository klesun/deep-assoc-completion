<?php
namespace RbsVer\Process\Apollo\ImportPnr;
use RbsVer\Process\Common\ImportPnr\ImportPnrCommonFormatAdapter;

/**
 * transforms output of the ImportApolloPnrAction to a common for any GDS structure
 */
class ImportApolloPnrFormatAdapter
{
    public static function transformReservation()
    {
        $reservation['passengers'] = [];
        $reservation = ImportPnrCommonFormatAdapter::addContextDataToPaxes($reservation);
        return $reservation;
    }
}
