<?php
namespace DeepTest;

class Naruto implements IKonohaCitizen
{
    private $money = 1000;

    private function __construct()
    {
    }

    public static function kageBunshin()
    {
        return new self();
    }

    public function payForDinner(string $price)
    {
        if ($this->money >= $price) {
            $this->money -= $price;
            return [
                'currency' => 'JPY',
                'amount' => $price,
            ];
        } else {
            throw new \Exception('Throw naruto in a prison!');
        }
    }

    public function payTaxes()
    {
        return [
            'currency' => 'JPY',
            'incomeTax' => '1', // Naruto has tiny income
            'gamblingTax' => '300',
            'familyTax' => '0', // Naruto has no family
        ];
    }
}