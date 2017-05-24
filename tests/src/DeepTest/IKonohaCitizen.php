<?php
namespace DeepTest;

interface IKonohaCitizen
{
    /**
     * @return array like [
     *     'currency' => 'JPY',
     *     'incomeTax' => '2050',
     *     'gamblingTax' => '1500',
     *     'familyTax' => '84620',
     * ]
     */
    public function payTaxes();
}