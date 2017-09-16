<?php
namespace TouhouNs;

class MarisaKirisame
{
    public function __construct($params)
    {
        $this->ability = $params['ability']; // "Master Spark", and the other one
        $this->bombsLeft = $params['bombsLeft'];
        $this->livesLeft = $params['livesLeft'];
        $this->power = $params['power'];
    }
}