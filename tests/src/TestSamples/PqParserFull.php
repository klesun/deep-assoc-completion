<?php
namespace Rbs\Parsers\Sabre\Pricing;
use Lib\Utils\Fp;
use Lib\Utils\StringUtil;
use Rbs\Parsers\Apollo\CommonParserHelpers;
use Rbs\Parsers\Sabre\BagAllowanceParser;

/**
 * parses the output of *PQ command (Price Quote - analog of *LF in Apollo)
 */
class PqParserFull
{
    public static function parse(string $dump): array
    {
        $lines = StringUtil::lines($dump);
        if (count($lines) === 1 && preg_match('/^.*NO PQ RECORD SUMMARY OR DETAIL EXISTS.*$/', $lines[0])) {
            return ['pqList' => []];
        } else if (trim($line = array_shift($lines)) !== 'PRICE QUOTE RECORD - DETAILS') {
            return ['error' => 'unexpectedStartOfDump', 'line' => $line];
        }
        array_shift($lines); // empty line

        while ($lastLine = array_pop($lines)) {
            if (!preg_match('/^ *$/', $lastLine)) {
                $lines[] = $lastLine;
                break;
            }
        }

        $parts =
            array_filter(Fp::map(function($tuple) {
                list($sectionLines, $emptyLine) = $tuple;
                return $sectionLines;
            }, self::splitByPred(function (string $l) {
                return preg_match('/^ *(DISPLAY ONLY)? *$/', $l);
            }, $lines)));

        $maybeDisclaimerLine = $parts[0][0] ?? null;
        if (rtrim($maybeDisclaimerLine) === 'FARE NOT GUARANTEED UNTIL TICKETED') {
            array_shift($parts);
        }

        $pqList =
            Fp::map(function($tuple) {
                list($pqContent, $pqHeader) = $tuple;
                return self::parseSinglePq($pqHeader, $pqContent);
            }, array_slice(self::splitByPred(function(array $sectionLines) {
                $maybePqHeader = $sectionLines[0] ?? '';
                return self::parsePqHeader($maybePqHeader) ? true : false;
            }, $parts), 1));

        if (count($pqList) > 0) {
            return ['pqList' => $pqList];
        } else {
            return ['error' => 'Dump does not contain Price Quotes: '.trim(implode(PHP_EOL, Fp::flatten($parts)))];
        }
    }

    private static function parseSinglePq(array $headerLines, array $sections)
    {
        $headerParse = self::parsePqHeader(implode('', $headerLines));

        $lastSection = array_pop($sections);
        $footerParse = self::parsePqFooter(array_pop($lastSection));
        $sections[] = $lastSection;

        $priceSection = array_shift($sections);
        while ($optionalSection = array_shift($sections)) {
            if ($priceSection && preg_match('/^\*\* {61}$/', $optionalSection[0])) {
                $priceSection = array_merge($priceSection, $optionalSection);
            } else if (StringUtil::startsWith($optionalSection[0], 'BAG ALLOWANCE')) {
                $baggageInfoDumpLines = $optionalSection;
                $baggageInfo = BagAllowanceParser::parse($optionalSection);
            } else if (StringUtil::startsWith($optionalSection[0], 'ONE OR MORE FORM OF PAYMENT FEES MAY APPLY')) {
                $feeCodes = $optionalSection;
            } else {
                throw new \Exception('unexpected section in single pq '.implode('', $headerLines).PHP_EOL.implode(PHP_EOL, $optionalSection));
            }
        }

        return [
            'pqNumber' => $headerParse['pqNumber'],
            'pricingModifiers' => $headerParse['pricingModifiers'],
            'priceInfo' => $priceSection ? self::parsePriceSection($priceSection) : null,
            'baggageInfo' => $baggageInfo ?? null,
            'baggageInfoDump' => isset($baggageInfoDumpLines)
                ? implode(PHP_EOL, $baggageInfoDumpLines)
                : null,
            'feeCodes' => $feeCodes ?? null,
            'footerInfo' => $footerParse,
        ];
    }

    public static function parsePricingQualifiers(string $qualifiers)
    {
        $parsedQualifiers = [];
        foreach ($qualifiers ? explode('¥', $qualifiers) : [] as $qualifier) {
            list($name, $data) = self::parsePricingQualifier($qualifier);
            $parsedQualifiers[] = [
                'raw' => $qualifier,
                'type' => $name,
                'parsed' => $data,
            ];
        }

        return $parsedQualifiers;
    }

    //'PQ 2  PL¥RQ¥KP0¥ABA¥ETR',
    private static function parsePqHeader(string $line)
    {
        $matches = [];
        if (preg_match('/^PQ([\d ]{4})(.*)$/', $line, $matches)) {
            list($_, $pqNumber, $qualifiers) = $matches;

            return [
                'pqNumber' => intval(trim($pqNumber)),
                'pricingModifiers' => self::parsePricingQualifiers(trim($qualifiers)),
            ];
        } else {
            return null;
        }
    }

    private static function parsePriceSection(array $lines)
    {
        $headersLine = array_shift($lines);
        if (trim($headersLine) === 'DISPLAY ONLY') {
            $headersLine = array_shift($lines);
        }
        if (preg_match('/^\s*BASE FARE.*TOTAL\s*$/', $headersLine)) {
            list($fareInfo, $lines) = PricingCommonHelper::parseFareInfo($lines);
            $fareBasisInfo = PricingCommonHelper::parseFareBasisSummary(array_shift($lines));
            list($lastDayToPurchase, $lines) = self::parseLastDayToPurchase($lines);
            list($fareConstruction, $lines) = self::parseFareConstruction($lines);
            list($fareConstructionInfo, $segments, $additionalInfo) = self::parseAdditionalInfoAndSegments($lines);

            return [
                'type' => 'normalPq',
                'totals' => $fareInfo['totals'] ?? null,
                'taxList' => $fareInfo['taxList'] ?? null,
                'fareBasisInfo' => $fareBasisInfo,
                'lastDayToPurchase' => $lastDayToPurchase,
                'fareConstruction' => $fareConstruction,
                'fareConstructionInfo' => $fareConstructionInfo,
                'segments' => $segments,
                'additionalInfo' => $additionalInfo,
            ];
        } elseif (self::parseSegmentLine($headersLine)) {
            array_unshift($lines, $headersLine);
            list($_, $segments, $additionalInfo) = self::parseAdditionalInfoAndSegments($lines);
            return [
                'type' => 'justItinerary',
                'segments' => $segments,
                'additionalInfo' => $additionalInfo,
            ];
        } else {
            return ['error' => 'failedToParse'];
        }
    }

    private static function parseLastDayToPurchase(array $lines): array
    {
        $matches = [];
        $line = array_shift($lines);
        if (preg_match('/^LAST DAY TO PURCHASE ([0-9]{2}[A-Z]{3})\/(\d{4}) *$/', $line, $matches)) {
            list($_, $date, $time) = $matches;
            $lastDayToPurchase = [
                'date' => [
                    'raw' => $date,
                    'parsed' => CommonParserHelpers::parsePartialDate($date)->getUsingDefault(null),
                ],
                'time' => [
                    'raw' => $time,
                    'parsed' => CommonParserHelpers::decodeApolloTime($time)->getUsingDefault(null),
                ],
            ];
        } else {
            array_unshift($lines, $line);
        }

        return [$lastDayToPurchase ?? null, $lines];
    }

    private static function parseFareConstruction(array $lines): array
    {
        $fcLines = array_splice($lines, 0);
        $parsed = null;
        while ($fcLines) {
            $parsed = PricingCommonHelper::parseFareConstructionLines($fcLines);
            if ($parsed['error'] || $parsed['textLeft']) {
                array_unshift($lines, array_pop($fcLines));
            } else {
                break;
            }
        }

        if (isset($parsed['error'])) {
            list($fcLines, $lines) = self::extractFareConstructionRoughly($lines);
            $parsed['line'] = implode('', $fcLines);
        }

        return [$parsed, $lines];
    }

    //"COMM AMT 68.84",
    //"PRIVATE ¤",
    //"FARE SOURCE - ATPC"
    private static function parseAdditionalInfo(array $lines)
    {
        $result = [
            'privateFareType' => null,
            'tourCode' => null,
            'commission' => null,
            'unparsed' => [],
        ];

        foreach ($lines as $line) {
            if (preg_match('/^\s*(PRIVATE\s+¤)\s*$/', $line, $matches)) {
                $result['privateFareType'] = [
                    'raw' => $matches[1],
                    'parsed' => 'filedWithoutAccountCode',
                ];
            } else if (preg_match('/^\s*(CORPID\s+\*)\s*$/', $line, $matches)) {
                $result['privateFareType'] = [
                    'raw' => $matches[1],
                    'parsed' => 'filedWithAccountCode',
                ];
            } else if (preg_match('/^\s*(CAT35\s+\/)\s*$/', $line, $matches)) {
                $result['privateFareType'] = [
                    'raw' => $matches[1], // likely that means "NET" fare
                    'parsed' => 'notFiledWithAccountCode',
                ];
            } else if (preg_match('/^\s*TOUR\s+CODE-(.*?)\s*$/', $line, $matches)) {
                $result['tourCode'] = $matches[1];
            } else if (preg_match('/^\s*COMM\s+(AMT|PCT)\s+(N\s+|)(\d*\.?\d+)\s*$/', $line, $matches)) {
                list($_, $units, $netFare, $value) = $matches;
                $result['commission'] = [
                    'units' => $units === 'PCT' ? 'percent' : 'amount',
                    'value' => $value,
                    'isAppliedToNetFare' => $netFare ? true : false,
                ];
            } else {
                $result['unparsed'][] = $line;
            }
        }

        return $result;
    }

    private static function parseSegmentLine(string $line)
    {
        // '03 O YYC WS 670G 19JUN  245P GPEUOL/ITEU     19JUN1719JUN17 NIL'
        $fullRegex =
            '/^'.
            '(?P<segmentNumber>\d{2})?\s+'.
            '(?P<stopoverIndicator>[OX])?\s+'.
            '(?P<airport>[A-Z]{3})\s+'.
            '(?P<airline>[A-Z0-9]{2})?\s*'.
            '(?P<flightNumber>\d{1,4})?'.
            '(?P<bookingClass>[A-Z])?\s+'.
            '(?P<departureDate>\d{2}[A-Z]{3})?\s+'.
            '(?P<departureTime>\d{3,4}[A-Z ])?\s+'.
            '(?:'.
                '(?P<fareBasis>[\dA-Z]+)'.
                '(?:\/(?P<ticketDesignator>[\dA-Z]+))?'.
            ')?\s+'.
            '(?P<notValidBefore>\d{2}[A-Z]{3}\d{2})?'.
            '(?P<notValidAfter>\d{2}[A-Z]{3}\d{2})?\s+'.
            '(?P<bagAllowanceCode>[A-Z\d]{3})?\s*'.
            '$/';

        if (preg_match('/^[A-Z]{3}$/', trim($line))) {
            return [
                'type' => 'lastAirport',
                'airport' => trim($line),
            ];
        } elseif (preg_match('/^\s*(\d+)\s+([A-Z]{3})\s+VOID\s*$/', $line, $matches)) {
            list($_, $number, $airport) = $matches;
            return [
                'type' => 'void',
                'segmentNumber' => $number,
                'airport' => $airport,
            ];
        } elseif (preg_match($fullRegex, $line, $matches)) {
            $notValidBefore = isset($matches['notValidBefore'])
                ? CommonParserHelpers::parseApolloFullDate($matches['notValidBefore'])->getUsingDefault(null)
                : null;
            $notValidAfter = isset($matches['notValidAfter'])
                ? CommonParserHelpers::parseApolloFullDate($matches['notValidAfter'])->getUsingDefault(null)
                : null;
            return [
                'type' => 'flight',
                'segmentNumber' => $matches['segmentNumber'] ?? null,
                'isStopover' => $matches['stopoverIndicator'] !== 'X',
                'airport' => $matches['airport'],
                'airline' => $matches['airline'] ?? null,
                'flightNumber' => $matches['flightNumber'] ?? null,
                'bookingClass' => $matches['bookingClass'] ?? null,
                'departureDate' => isset($matches['departureDate']) ? [
                    'raw' => $matches['departureDate'],
                    'parsed' => CommonParserHelpers::parsePartialDate($matches['departureDate'])->getUsingDefault(null),
                ] : null,
                'departureTime' => isset($matches['departureTime']) ? [
                    'raw' => $matches['departureTime'],
                    'parsed' => CommonParserHelpers::decodeApolloTime($matches['departureTime'])->getUsingDefault(null),
                ] : null,
                'fareBasis' => $matches['fareBasis'] ?? null,
                'ticketDesignator' => $matches['ticketDesignator'] ?? null,
                'notValidBefore' => [
                    'raw' => $matches['notValidBefore'] ?? null,
                    'parsed' => $notValidBefore ? '20'.$notValidBefore : null,
                ],
                'notValidAfter' => [
                    'raw' => $matches['notValidAfter'] ?? null,
                    'parsed' => $notValidAfter ? '20'.$notValidAfter : null,
                ],
                'freeBaggageAmount' => isset($matches['bagAllowanceCode'])
                    ? BagAllowanceParser::parseAmountCode($matches['bagAllowanceCode'])
                    : null,
                'line' => $line,
            ];
        } else {
            return null;
        }
    }

    private static function parseAdditionalInfoAndSegments(array $lines): array
    {
        $fareConstructionInfoLines = [];
        while ($line = array_shift($lines)) {
            if (!self::parseSegmentLine($line)) {
                $fareConstructionInfoLines[] = $line;
            } else {
                array_unshift($lines, $line);
                break;
            }
        }

        $segments = [];
        while ($line = array_shift($lines)) {
            if ($segment = self::parseSegmentLine($line)) {
                $segments[] = $segment;
            } else {
                array_unshift($lines, $line);
                break;
            }
        }

        $topAdditionalInfo = PricingCommonHelper::parseFareConstructionInfo($fareConstructionInfoLines, false);

        return [$topAdditionalInfo, $segments, self::parseAdditionalInfo($lines)];
    }

    //'Y6V0 Y6V0 *ASC 0029/04NOV15                           PRICE-SYS',
    private static function parsePqFooter(string $footerLine)
    {
        $matches = [];
        $regex =
            '/^'.
            '(?P<pcc>[A-Z0-9]{3,4}) +'.
            '(?P<homePcc>[A-Z0-9]{3,4}) +'.
            '(\*|\d|R|\-)'.
            '(?P<agentInitials>[A-Z0-9]{3}) '.
            '(?P<time>[0-9]{4})\/'.
            '(?P<date>\d{2}[A-Z]{3}\d{2})'.
            // usually just 27 spaces, but there is such example in docs:
            // "7DQ2 7DQ2 *ANU 1054/22MAR *ITIN CHG *              PRICE-SYS"
            '.{27}'.
            'PRICE-(?P<priceIndicator>SYS|AGT)'.
            '$/';

        if (preg_match($regex, $footerLine, $matches)) {
            $parsedDate = CommonParserHelpers::parseApolloFullDate($matches['date'])->getUsingDefault(null);
            return [
                'pcc' => $matches['pcc'],
                'homePcc' => $matches['homePcc'],
                'agentInitials' => $matches['agentInitials'],
                'createdOnTime' => [
                    'raw' => $matches['time'],
                    'parsed' => CommonParserHelpers::decodeApolloTime($matches['time'])->getUsingDefault(null),
                ],
                'createdOnDate' => [
                    'raw' => $matches['date'],
                    'parsed' => $parsedDate ? '20'.$parsedDate : null,
                ],
                'priceIndicator' => $matches['priceIndicator'],
                'line' => $footerLine,
            ];
        } else {
            return ['line' => $footerLine];
        }
    }

    /** @param $expr = '1/3/5-7/9' */
    private static function parseRanges(string $expr)
    {
        $parseRange = function($text){
            $pair = explode('-', $text);
            return range($pair[0], $pair[1] ?? $pair[0]);
        };
        return Fp::flatten(Fp::map($parseRange, explode('/', trim($expr))));
    }

     // 'N1.1/1.2/2.1'
     // 'N1.1-2.1'
     // 'N1.1-2.1/4.1-5.1/6.0'
    private static function parseNameQualifier(string $token)
    {
        if (!StringUtil::startsWith($token, 'N')) {
            return null;
        } else {
            $content = substr($token, 1);
        }

        $records = Fp::map(function($ptcToken){
            if (preg_match('/^(\d+\.\d+)(-\d+\.\d+|)$/', $ptcToken, $matches)) {
                list($_, $from, $to) = $matches;
                $to = substr($to, 1);
                return [
                    'fieldNumber' => explode('.', $from)[0],
                    'firstNameNumber' => explode('.', $from)[1],
                    'through' => $to ? [
                        'fieldNumber' => explode('.', $to)[0],
                        'firstNameNumber' => explode('.', $to)[1],
                    ] : null,
                ];
            } else {
                return null;
            }
        }, explode('/', $content));

        if (Fp::any('is_null', $records)) {
            return null;
        } else {
            return $records;
        }
    }

    /**
     * @param $token = 'S1/2-3*Q//DA25*PC05' ?? 'S1/3'
     * @return [1,2,3]
     */
    private static function parseSegmentQualifier(string $token)
    {
        $regex =
            '/^S'.
            '(?<segNums>\d+[\d\/\-]*)'.
            '(?<unparsed>\*.+|)'.
            '$/';
        if (preg_match($regex, $token, $matches)) {
            return [
                'segmentNumbers' => self::parseRanges($matches['segNums']),
                'unparsed' => $matches['unparsed'],
            ];
        } else {
            return null;
        }
    }

    // 'PADT', 'PINF'
    // 'PADT/CMP' // companion
    // 'PJCB/2JNF' // 1 JCB (adult) and 2 JNF (infants)
    // 'P1ADT/2C11/1ADT'
    private static function parsePtcQualifier(string $token)
    {
        if (!StringUtil::startsWith($token, 'P')) {
            return null;
        } else {
            $content = substr($token, 1);
        }

        $records = Fp::map(function($ptcToken){
            if (preg_match('/^(\d*)([A-Z0-9]{3})$/', $ptcToken, $matches)) {
                list($_, $cnt, $ptc) = $matches;
                $cnt = $cnt !== '' ? intval($cnt) : null;
                return ['quantity' => $cnt, 'ptc' => $ptc];
            } else {
                return null;
            }
        }, explode('/', $content));

        if (Fp::any('is_null', $records)) {
            return null;
        } else {
            return $records;
        }
    }

    /**
     * @see https://formatfinder.sabre.com/Content/Pricing/PricingOptionalQualifiers.aspx?ItemID=7481cca11a7449a19455dc598d5e3ac9
     */
    private static function parsePricingQualifier(string $token)
    {
        list($name, $data) = [null, null];

        if ($token === 'RQ') {
            list($name, $data) = ['createPriceQuote', true];
        } elseif ($token === 'ETR') {
            list($name, $data) = ['areElectronicTickets', true];
        } elseif (in_array($token, ['PL', 'PV'])) {
            list($name, $data) = ['fareType', $token === 'PL' ? 'public' : 'private'];
        } elseif ($data = self::parseNameQualifier($token)) {
            $name = 'names';
        } elseif ($data = self::parsePtcQualifier($token)) {
            $name = 'ptc';
        } elseif ($data = self::parseSegmentQualifier($token)) {
            $name = 'segments';
        } elseif (preg_match('/^PU\*(\d*\.?\d+)(\/[A-Z0-9]+|)$/', $token, $matches)) {
            list($name, $data) = ['markup', $matches[1]];
        } elseif (preg_match('/^K(P|)([A-Z]*)(\d*\.?\d+)$/', $token, $matches)) {
            list($_, $percentMarker, $region, $amount) = $matches;
            list($name, $data) = ['commission', [
                'units' => $percentMarker ? 'percent' : 'amount',
                'region' => $region ?: null,
                'value' => $amount,
            ]];
        } elseif (preg_match('/^A([A-Z0-9]{2})$/', $token, $matches)) {
            list($name, $data) = ['validatingCarrier', $matches[1]];
        } elseif (preg_match('/^C-([A-Z0-9]{2})$/', $token, $matches)) {
            list($name, $data) = ['governingCarrier', $matches[1]];
        } elseif (preg_match('/^M([A-Z]{3})$/', $token, $matches)) {
            list($name, $data) = ['currency', $matches[1]];
        } elseif (preg_match('/^AC\*([A-Z0-9]+)$/', $token, $matches)) {
            list($name, $data) = ['accountCode', $matches[1]];
        } elseif (preg_match('/^ST(\d+[\d\/\-]*)$/', $token, $matches)) {
            list($name, $data) = ['sideTrip', self::parseRanges($matches[1])];
        } elseif (preg_match('/^W(TKT)$/', $token, $matches)) {
            list($name, $data) = ['exchange', $matches[1]];
        } elseif (preg_match('/^NC$/', $token, $matches)) {
            list($name, $data) = ['lowestFare', true];
        } elseif (preg_match('/^NCS$/', $token, $matches)) {
            list($name, $data) = ['lowestFareIgnoringAvailability', true];
        } elseif (preg_match('/^NCB$/', $token, $matches)) {
            list($name, $data) = ['lowestFareAndRebook', true];
        }

        return [$name, $data];
    }

    private static function extractFareConstructionRoughly(array $lines)
    {
        $fcLines = array_splice($lines, 0);

        $fcEndRegex = '/'.
            '([^A-Z][A-Z]{3}\d*\.?\d+(\s+PLUS\d*\.?\d+\s*)?(END)?)?'. // "NUC213.01END"
            '(\s+ROE\d*\.?\d+)?'. // "ROE0.765204"
            '(\s+(\d*\.?\d+)?XF([A-Z]{3}\d*\.?\d+)+)?'. // "XFLAS4.5"
        '$/';

        while ($fcLines) {
            if (preg_match($fcEndRegex, implode('', $fcLines), $matches)) {
                if (trim($matches[0]) !== '') {
                    break;
                }
            }
            array_unshift($lines, array_pop($fcLines));
        }

        return [$fcLines, $lines];
    }

    private static function splitByPred(callable $pred, array $elements)
    {
        $idxs = array_keys(array_filter($elements, $pred));

        $result = [];
        foreach (array_reverse($idxs) as $i) {
            $result[] = [
                array_splice($elements, $i + 1),
                array_splice($elements, $i)[0],
            ];
        }
        $result[] = [$elements, null];

        return array_reverse($result);
    }
}
