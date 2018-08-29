<?php
namespace DeepTest;

class Konohamaru implements IKonohaCitizen
{
    public function payTaxes()
    {
        // Konohamaru is a kid, so he doesn't need to pay taxes
        return [
            'currency' => 'JPY',
            'incomeTax' => '0',
            'gamblingTax' => '0',
            'familyTax' => '0',
        ];
    }

    public function getHonestOpinion()
    {
        return json_decode(file_get_contents(__DIR__.'/honest_opinion.json'));
    }
}