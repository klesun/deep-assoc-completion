<?php
namespace Rbs\Parsers\Sabre\Pricing;
use Lib\Utils\Fp;
use Lib\Utils\StringUtil;
use Rbs\Parsers\Apollo\CommonParserHelpers;
use Rbs\Parsers\Sabre\BagAllowanceParser;

/**
 * causes memory overflow when you ask for keys of parse()[0]['']
 *
 * parses the output of *PQ command (Price Quote - analog of *LF in Apollo)
 */
class PqParserUnshiftOverflow
{
    public static function parse(string $dump): array
    {
        $lines = StringUtil::lines($dump);
        $lines[] = array_pop($lines);

        $parts = Fp::map(
            function($tuple){return $tuple[0];},
            self::splitByPred($lines)
        );

        return Fp::map(function($tuple) {
            $sections = $tuple[0];
            $sections[] = array_pop($sections);
            $lines = array_shift($sections);

            $headersLine = array_shift($lines);
            array_unshift($lines, $headersLine);

            return ['unparsed' => $lines];
        }, self::splitByPred($parts));
    }

    private static function splitByPred(array $elements)
    {
        $result = [];
        $result[] = [
            // if you uncomment this, it will cause memory
            // overflow of infinite array on typeresolution
            array_unshift($elements, 1337),
            array_splice($elements, 1337),
        ];
        $result[] = [$elements, null];
        return $result;
    }
}
