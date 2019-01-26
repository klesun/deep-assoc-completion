<?php
namespace RbsVer;

class UnitTest
{
    public function provideImportPnr()
    {
        $imported = \RbsVer\Process\Common\ImportPnr\ImportPnrAction::makeByGds([
            'gdsName' => 'apollo',
            'recordLocator' => 'qwe123',
            'pnrFields' => ['reservation'],
        ])->execute();
        $imported[''];
        $imported['result'][''];
        $imported['result']['pnrFields'][''];
        $imported['result']['pnrFields']['reservation'][''];
        $imported['result']['pnrFields']['reservation']['itinerary'][0][''];
        $imported['result']['pnrFields']['reservation']['passengers'][0][''];
        $imported['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'][''];
        $imported['result']['pnrFields']['fareQuoteInfo'][''];
        $imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0][''];
        $imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0]['pricingBlockList'][0][''];

        return [
            [$imported, ['response_code' => [], 'errors' => [], 'result' => []]],
            [$imported['result'], ['accessStatus' => [], 'fieldsAvailable' => [], 'pnrFields' => [], 'dumps' => [], 'errorData' => []]],
            [$imported['result']['pnrFields'], ['reservation' => [], 'fareQuoteInfo' => [], 'ticketInfo' => [], 'ticketingCorrectFareRules' => []]],
            [$imported['result']['pnrFields']['reservation'], ['passengers' => [], 'itinerary' => [], 'pnrInfo' => []]],
            [$imported['result']['pnrFields']['reservation']['itinerary'][0], ['departureDt' => [], 'airline' => [], 'destinationAirport' => []]],
            [$imported['result']['pnrFields']['reservation']['passengers'][0], ['nameNumber' => [], 'lastName' => [], 'firstName' => []]],
            [$imported['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'], ['isInfant' => [], 'firstNameNumber' => [], 'fieldNumber' => [], 'absolute' => []]],
            [$imported['result']['pnrFields']['fareQuoteInfo'], ['pricingList' => [], 'error' => []]],
            [$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0], ['quoteNumber' => [], 'pricingPcc' => [], 'pricingModifiers' => [], 'pricingBlockList' => []]],
            [$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0]['pricingBlockList'][0], ['passengerNameNumbers' => [], 'ptcInfo' => [], 'fareInfo' => [], 'validatingCarrier' => []]],
            // TODO: continue, add fare construction and stuff
        ];
    }
}
