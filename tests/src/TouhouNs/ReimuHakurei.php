<?php
namespace TouhouNs;

class ReimuHakurei
{
    private $oneTrueLove = 'Yukari';

    public static function fantasySeal()
    {
        return [
            'invulnerability' => 3.5,
            'magicSpheres' => [
                ['color' => 'red', 'damage' => 300],
                ['color' => 'green', 'damage' => 200],
                ['color' => 'blue', 'damage' => 250],
            ],
        ];
    }

    public static function evilSealingCircle()
    {
        return [
            'missileDensity' => '10 per second',
            'missileDamage' => '200',
            'arcDegree' => '45',
        ];
    }

    public function demandDonuts()
    {
        return [
            'patience' => 0.01,
            'amount' => 10,
            'consequences' => 'juggernaut',
        ];
    }
}