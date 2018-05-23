<?php
namespace Gtl\AmadeusSoapActions;
/**
 * get fare rules without pricing - using from/to/fareBasis/etc...
 * returns all sections no matter how much text they contain
 */
class AmadeusGetFareRulesAction extends \Gtl\AmadeusSoapActions\AbstractAmadeusSoapAction
{
    const SOAP_FUNCTION = 'Fare_GetFareRules';
    const SOAP_ACTION = 'http://webservices.amadeus.com/FARRNQ_10_1_1A';
    protected static function transformRequestParams(array $params)
    {
        $origin = $params['origin'];
        $destination = $params['destination'];
        $ticketingDt = $params['ticketingDt'];
        $airline = $params['airline'];
        $departureDt = $params['departureDt'];
        $fareBasis = $params['fareBasis'];
        $ticketDesignator = $params['ticketDesignator'] ?? null;
        return [
            'msgType' => ['messageFunctionDetails' => ['messageFunction' => 'FRN']],
            'pricingTickInfo' => ['productDateTimeDetails' => ['ticketingDate' => date('dmy', strtotime($ticketingDt))]],
            'flightQualification' => [
                'additionalFareDetails' => ['rateClass' => $fareBasis, 'commodityCategory' => $ticketDesignator],
            ],
            'transportInformation' => ['transportService' => ['companyIdentification' => ['marketingCompany' => $airline]]],
            'tripDescription' => [
                'origDest' => ['origin' => $origin, 'destination' => $destination],
                'dateFlightMovement' => ['dateAndTimeDetails' => ['date' => date('dmy', strtotime($departureDt))]],
            ],
        ];
    }
    protected static function transformResponse(\stdClass $data)
    {
        return $data;
    }
}