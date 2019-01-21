<?php
namespace Rbs\Process\Apollo\ImportPnr;
use Rbs\Process\Common\ImportPnr\IGdsPnrFieldsProvider;

class ApolloPnrFieldsOnDemand implements IGdsPnrFieldsProvider
{
    public function getReservation()
    {
        // must pass some args, won't reproduce otherwise
        return ImportApolloPnrFormatAdapter::transformReservation($this->reservationInfo, $this->baseDate);
    }
}
