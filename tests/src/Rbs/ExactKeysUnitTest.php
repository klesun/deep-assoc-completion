<?php

class ExactKeysUnitTest
{
    private function importPnr()
    {
        $imported = (new \Rbs\Process\Common\ImportPnr\ImportPnrAction)->execute();
        $imported['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'][''];

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
                'fieldName',                               'serviceSsrList',
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
            ]],
        ];
    }

    public function provideImportPnrNext01()
    {
        $this->importPnr()['result']['pnrFields']['reservation']['passengers'][0][''];
        return [
            [$this->importPnr()['result']['pnrFields']['reservation']['passengers'][0], ['ageGroup', 'firstName', 'title', 'nameNumber', 'lastName', 'age', 'success', 'rawNumber', 'dob', 'ptc', 'carrierText', 'parsedNumber', 'joinedFirstNames']],
        ];
    }

    public function provideImportPnrNext02()
    {
        $this->importPnr()['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'][''];
        return [
            [$this->importPnr()['result']['pnrFields']['reservation']['passengers'][0]['nameNumber'], ['firstNameNumber', 'fieldNumber', 'absolute', 'raw', 'isInfant']],
            //[$imported['result']['pnrFields']['fareQuoteInfo'], ['pricingList' => [], 'error' => []]],
            //[$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0], ['quoteNumber' => [], 'pricingPcc' => [], 'pricingModifiers' => [], 'pricingBlockList' => []]],
            //[$imported['result']['pnrFields']['fareQuoteInfo']['pricingList'][0]['pricingBlockList'][0], ['passengerNameNumbers' => [], 'ptcInfo' => [], 'fareInfo' => [], 'validatingCarrier' => []]],
        ];
    }
    // TODO: continue, add fare construction and stuff
}
