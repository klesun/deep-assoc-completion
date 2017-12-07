<?php
namespace TestSamples\CmsSessionMemoryOverflow;

use Lib\Db;
use Lib\FluentLogger;
use Lib\Utils\Fp;
use Lib\Utils\StringUtil;
use Rbs\GdsAction\ApolloBuildItineraryAction;
use Rbs\GdsAction\SabreBuildItineraryAction;
use Rbs\GdsAction\Traits\TSabreSavePnr;
use Rbs\GdsDirect\Errors;
use Rbs\GdsDirect\GetPqItineraryAction;
use Rbs\GdsDirect\ITerminalCommandLogRead;
use Rbs\GdsDirect\SavePnrProcessor;
use Rbs\GdsDirect\SeatsTaken;
use Rbs\GdsDirect\SessionStateHelper;
use Rbs\IqControllers\CmsTerminalController;
use Rbs\Parsers\Apollo\ApolloReservationParser\ApolloReservationParser;
use Rbs\Parsers\Sabre\Pricing\SabrePricingParser;
use Rbs\Parsers\Sabre\SabreReservationParser\ItineraryParser;
use Rbs\Process\Amadeus\Terminal\UpdateAmadeusSessionStateAction;
use Rbs\Process\Apollo\ImportPnr\Actions\RepeatItineraryAction;
use Rbs\Process\Common\ImportPnr\ImportPnrAction;
use Rbs\Transport\ApolloStatelessTerminal;

class SessionStateProcessor
{
    public static function updateFromArea(array $sessionData, array $areaData)
    {
        if ($token = $areaData['internal_token'] ?? null) {
            $sessionData['internal_token'] = $token;
        }
        return $sessionData;
    }

    /** "safe" means it does not write to DB */
    public static function updateStateSafe(string $cmd, string $output, array $sessionState, callable $getAreaData)
    {
        $sessionState = self::updateSessionForEachCommand($parsed['cmd'], $output, $sessionState, $getAreaData);

        foreach ($parsed['followingCommands'] ?? [] as $subCmd) {
            $sessionState = self::updateSessionForEachCommand($subCmd['cmd'], $output, $sessionState, $getAreaData);
        }
        return $sessionState;
    }

    private static function updateSessionForEachCommand($cmd, $output, $sessionState, callable $getAreaData)
    {
        if (rand() % 2) {
            $sessionState = self::handleSabreIgnoreAndCopyPnr($sessionState, $output);
        } elseif (rand() % 2) {
            $areaData = $getAreaData($data);
            $sessionState = self::updateFromArea($sessionState, $areaData);
        }
        return $sessionState;
    }

    private static function handleSabreIgnoreAndCopyPnr(array $sessionData, string $output): array
    {
        if (trim($output) === 'IGD') {
            $sessionData['record_locator'] = null;
            $sessionData['is_pnr_stored'] = false;
        }
        return $sessionData;
    }
}
