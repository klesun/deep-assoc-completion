<?php
namespace TouhouNs;

class YakumoRan
{
    private $shikigamis = [];
    public $love = 50;

    private static function normalizeShikigami($shiki)
    {
        return [
            'name' => $shiki['name'],
            'power' => $shiki['power'],
        ];
    }

    public function acquireJuniorDevelopers(array $juniors)
    {
        $normed = array_map([self::class, 'normalizeShikigami'], $juniors);
        $this->shikigamis = array_merge($this->shikigamis, $normed);
        $this->love -= pow(2, count($juniors));
        return $this;
    }

    public function acquireChen($chen)
    {
        $this->shikigamis[] = self::normalizeShikigami($chen);
        $this->love += 99999999999999999999;
        return $this;
    }

    public function getFreeShikigami()
    {
        return array_pop($this->shikigamis);
    }
}