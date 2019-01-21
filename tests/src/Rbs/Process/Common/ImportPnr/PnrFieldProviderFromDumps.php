<?php
namespace Rbs\Process\Common\ImportPnr;
use Rbs\Process\Apollo\ImportPnr\ImportApolloPnrFormatAdapter;

/**
 * it behaves same as normal (online) PNR fields provider, but uses initially
 * provided dumps and includes some workaround logic like guessing Sabre
 * pricing -> pax mapping from PTC instead of calling additional >*PQS;
 */
class PnrFieldProviderFromDumps implements IGdsPnrFieldsProvider
{

    // ===============================================
    //  each following function corresponds to a PNR field
    // ===============================================

    public function getReservation()
    {
        // it must be put in a var, does not reproduce otherwise
        $transformed = ImportApolloPnrFormatAdapter::transformReservation();
        return $transformed;
    }
}
