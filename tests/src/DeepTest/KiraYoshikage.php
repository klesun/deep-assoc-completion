<?php
namespace DeepTest;

class KiraYoshikage
{
    private $passionCriteria = [
        'nailLength' => 15,
        'skinType' => ['smooth', 'soft'],
        'handCount' => 1,
    ];
    private $badassness;

    public function __construct()
    {
        $this->characterProps = [
            'powers' => 'average',
            'story' => 'good',
            'personality' => 'topvishkaogonj',
        ];
    }

    private function getBadassness()
    {
        return $this->badassness
            ?? $this->badassness = [
                'randomPeopleKilled' => 50,
                'goodGuysKilled' => 3,
                'sufferBrought' => 0.43,
                'womenSeduced' => 49,
                'boom' => self::bombTransmutation(),
            ];
    }

    public static function bombTransmutation()
    {
        return [
            'turns' => 123,
            'anything' => '35',
            'i' => '23423',
            'touch' => [
                'into' => 'adad',
                'a' => '345',
                'bomb' => 'k2onnoia',
            ],
        ];
    }

    public static function sheerHeartAttack()
    {
        return [
            'veryTough' => 134,
            'smallCar' => [
                'that' => 123,
                'follows' => 252,
                'heat' => '4564',
                'and' => ['explodes', 'explodes', 'explodes'],
            ],
        ];
    }

    public static function bitesZaDusto()
    {
        return [
            'time' => 1231,
            'goes' => 1231,
            'back' => 1231,
        ];
    }

    /**
     * @param array $woman = [
     *   'nailLength' => 15,
     *   'skinType' => ['smooth', 'soft'],
     *   'handCount' => 1,
     * ]
     */
    public function murder($woman)
    {
        if ($woman['nailLength'] > $this->passionCriteria['nailLength'] &&
            !array_diff($this->passionCriteria['skinType'], $woman['skinType']) &&
            $woman['handCount'] > $this->passionCriteria['handCount']
        ) {
            self::sheerHeartAttack();
        }

        if ($this->characterProps['personality'] >= 'dio' ||
            $this->characterProps['powers'] >= 'damon' ||
            $this->characterProps['story'] >= 'aztecArc'
        ) {
            self::bombTransmutation();
        }
    }
}