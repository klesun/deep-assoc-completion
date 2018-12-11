<?php

class ExactKeysUnitTest
{
    private function importPnr()
    {
        $imported = \Rbs\Process\Common\ImportPnr\ImportPnrAction::makeByGds([
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
        return $imported;
    }

    public function provideImportPnrHeader()
    {
        $imported = $this->importPnr();

        return [
            [$imported, ['response_code', 'errors', 'result']],
            [$imported['result'], ['accessStatus', 'fieldsAvailable', 'pnrFields', 'dumps', 'errorData']],
            [$imported['result']['pnrFields'], [
                'fieldName',
                'flightServiceInfo',                       'sample',
                'frequentFlyerInfo',                       'reservation',
                'seatInfo',                                'cars',
                'seatMap',                                 'hotels',
                'ticketInfo',                              'formOfPaymentInfo',
                'ticketInvoiceInfo',                       'pricingStoreList',
                'mcoData',                                 'fareQuoteInfo',
                'ticketHistoricalImage',                   'baggageQuoteInfo',
                'marriage',                                'restoredPricing',
                'ticketingAgentInfo',                      'publishedPricing',
                'bookingPccInfo',                          'publishedPricingIfNeeded',
                'pricingPccInfo',                          'ticketingCorrectPricingIfNeeded',
                'itineraryHistory',                        'ticketingCorrectFareRules',
                'wholeHistory',                            'ticketingCorrectBaggageQuoteInfo',
                'fcSegmentMapping',                        'fareComponentListInfo',
                'destinationsFromLinearFare',              'exchangeFareRules',
                'destinationsFromTicketingCorrectPricing', 'minStayFareRules',
                'destinationsFromStayTime',                'maxStayFareRules',
                'itineraryUtcTimes',                       'voluntaryChangeFareRules',
                'detectedTripType',                        'summedFareRules',
                'validatingCarrierFromItinerary',          'verifyConnectionTimes',
                'transborderAvailabilityInfo',             'repeatedItinerary',
                'contractInfo',                            'docSsrList',
                'forcedHistoricFare',                      'serviceSsrList',
            ]],
        ];
    }

    public function provideImportPnrNext01()
    {
        return [
            [$this->importPnr()['result']['pnrFields']['reservation']['passengers'][0], ['ageGroup', 'firstName', 'title', 'nameNumber', 'lastName', 'age', 'success', 'rawNumber', 'dob', 'ptc', 'carrierText', 'parsedNumber', 'joinedFirstNames']],
        ];
    }

    public function provideImportPnrNext02()
    {
        return [
            [$this->importPnr()['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'], ['firstNameNumber', 'fieldNumber', 'absolute', 'raw', 'isInfant']],
            //[$imported['result']['pnrFields']['fareQuoteInfo'], ['pricingList' => [], 'error' => []]],
            //[$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0], ['quoteNumber' => [], 'pricingPcc' => [], 'pricingModifiers' => [], 'pricingBlockList' => []]],
            //[$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0]['pricingBlockList'][0], ['passengerNameNumbers' => [], 'ptcInfo' => [], 'fareInfo' => [], 'validatingCarrier' => []]],
        ];
    }
    // TODO: continue, add fare construction and stuff
}
