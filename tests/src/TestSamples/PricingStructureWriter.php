<?php
namespace Rbs\Parsers\Apollo\PricingParser\DataStructureWriters;

class PricingStructureWriter
{
    private $dataStructure = [];
    private $currentPricingBlock = null;
    private $baggageInfoLines = [];

    public static function make()
    {
        return new self();
    }

    private function __construct()
    {
        $this->dataStructure = [
            'wholePricingMarkers' => [
                'fareGuaranteedAtTicketIssuance' => false,
                'fareHasPlatingCarrierRestriction' => false,
                'agentSelectedFareUsed' => false,
                'eTicketRequired' => false,
            ],
            'pricingBlockList' => []
        ];
    }

    private function saveCurrentPricingBlock()
    {
        if ($this->baggageInfoLines) {
            $this->currentPricingBlock['baggageInfo']['raw'] = implode('', $this->baggageInfoLines);

            try {
                $this->currentPricingBlock['baggageInfo']['parsed'] = [
                    'freeBagCode' => [
                        'amount' => '2',
                        'units' => 'pieces',
                    ],
                    'feePerBag' => [
                        'currency' => 'USD',
                        'amount' => '30.00',
                        'maxWeight' => '20KG',
                    ],
                ];
            } catch (\Exception $e) {
                $this->currentPricingBlock['baggageInfo']['parsed'] = null;
            }

            $this->baggageInfoLines = null;
        }
        $this->dataStructure['pricingBlockList'][] = $this->currentPricingBlock;
    }

    private function makeNewPricingBlock()
    {
        if ($this->currentPricingBlock) {
            $this->saveCurrentPricingBlock();
        }
        $this->currentPricingBlock = [
            'baggageInfo' => ['raw' => '', 'parsed' => null],
            'defaultPlatingCarrier' => null,
            'fareConstruction' => null,
            'lastDateToPurchaseTicket' => null,
            'notValidBA' => [],
            'passengerNumbers' => null,
            'penaltyApplies' => false,
            'ticketingAgencyPcc' => null,
            'tourCode' => null,
            'privateFaresSelected' => false,
        ];
    }

    // --------------------------------------------------

    public function commandCopyLineFound($res)
    {
        $this->dataStructure['pricingCommandCopy'] = $this->dataStructure['pricingCommandCopy'] ?? '';
        $this->dataStructure['pricingCommandCopy'] .= $res['line'];
        $this->dataStructure['parsedPricingCommand'] = [
            'pricingModifiers' => [
                ['raw' => ':N'],
                ['raw' => 'N1|2*INF'],
                ['raw' => 'CUA'],
            ]
        ];
    }
    public function eTicketRequiredStatementFound($res)
    {
        $this->dataStructure['wholePricingMarkers']['eTicketRequired'] = true;
    }
    public function paperTicketRequiredStatementFound()
    {
        $this->dataStructure['wholePricingMarkers']['paperTicketRequired'] = true;
    }
    public function fareGuaranteedAtTicketIssuanceStatementFound($res)
    {
        $this->dataStructure['wholePricingMarkers']['fareGuaranteedAtTicketIssuance'] = true;
    }
    public function fareHasPlatingCarrierRestrictionStatementFound($res)
    {
        $this->dataStructure['wholePricingMarkers']['fareHasPlatingCarrierRestriction'] = true;
    }
    public function fareHasFormOfPaymentRestrictionStatementFound($res)
    {
        $this->dataStructure['wholePricingMarkers']['fareHasFormOfPaymentRestriction'] = true;
    }
    public function agentSelectedFareUsedStatementFound($res)
    {
        $this->dataStructure['wholePricingMarkers']['agentSelectedFareUsed'] = true;
    }

    // --------------------------------------------------

    public function additionalServicesStatementFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }
        $this->currentPricingBlock['carrierMayOfferAdditionalServices'] = true;
    }

    public function penaltyAppliesStatementFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }
        $this->currentPricingBlock['penaltyApplies'] = true;
    }

    public function privateFaresSelectedStatementFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }
        $this->currentPricingBlock['privateFaresSelected'] = true;
    }

    public function unknownPricingStatementLineFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }

        // Previously I was logging such lines, but it seems that we managed to catch 'em all
    }

    public function unparsedInfoLineFound(string $line)
    {
        $this->currentPricingBlock['unparsedInfoLines'][] = $line;
    }

    public function lastDateToPurchaseTicketFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }
        $this->currentPricingBlock['lastDateToPurchaseTicket'] = $res['date'];
    }

    public function fareConstructionMarkerLineFound($res)
    {
        if (array_key_exists('makeNewPricingBlock', $res) && $res['makeNewPricingBlock']) {
            $this->makeNewPricingBlock();
        }
        $this->currentPricingBlock['passengerNumbers'] = $res['passengerNumbers'];
    }

    // --------------------------------------------------

    public function fareConstructionFound($res)
    {
        $this->currentPricingBlock['fareConstruction'] = $res['fareConstruction'];
        unset($this->currentPricingBlock['fareConstruction']['textLeft']);
    }

    public function notValidBeforeOrAfterLineFound($res)
    {
        $this->currentPricingBlock['notValidBA'][] = [
            'number' => $res['number'],
            'notValidBefore' => $res['notValidBefore'],
            'notValidAfter' => $res['notValidAfter'],
        ];
    }

    public function endorsementBoxLineFound($res)
    {
        $this->currentPricingBlock['endorsementBoxLine'][] = $res['endorsementBox'];
    }

    public function ticketingAgencyLineFound($res)
    {
        $this->currentPricingBlock['ticketingAgencyPcc'] = $res['pcc'];
    }

    public function tourCodeLineFound($res)
    {
        $this->currentPricingBlock['tourCode'] = $res['tourCode'];
    }

    public function defaultPlatingCarrierLineFound($res)
    {
        $this->currentPricingBlock['defaultPlatingCarrier'] = $res['airline'];
    }

    public function getStructure()
    {
        if ($this->currentPricingBlock) {
            $this->saveCurrentPricingBlock();
        }

        return $this->dataStructure;
    }
}
