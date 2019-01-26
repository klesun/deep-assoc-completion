<?php
namespace RbsVer\Process\Apollo\ImportPnr;
use RbsVer\Process\Common\ImportPnr\IGdsPnrFieldsProvider;

class ApolloPnrFieldsOnDemand implements IGdsPnrFieldsProvider
{
    public function getReservation()
    {
        // must pass some args, won't reproduce otherwise
        return ImportApolloPnrFormatAdapter::transformReservation($a);
    }
}
