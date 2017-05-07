<?php
namespace DeepTest;

class TestData
{
    public static function makeStudentRecord()
    {
        $record = [
            'id' => 123,
            'firstName' => 'Vasya',
            'lastName' => 'Pupkin',
            'year' => 2,
            'faculty' => 'programming',
            'pass' => [
                'birthDate' => '1995-09-01',
                'birthCountry' => 'Latvia',
                'passCode' => '123ABC',
                'expirationDate' => '2021-01-01',
                'family' => [
                    'spouse' => 'Perpetuya Pupkina',
                    'children' => [
                        'Valera Orlov',
                    ],
                ],
            ],
            'chosenSubSubjects' => array_map(function($i) {
                return [
                    'name' => 'philosophy_'.$i,
                    'priority' => 5.7 + $i,
                ];
            }, range(0,9)),
        ];
        $record['friends'][] = [
            'name' => 'Madao',
            'occupation' => 'Madao',
        ];
        $record['friends'][] = [
            'name' => 'Phoenix Wright',
            'occupation' => 'Madao',
        ];

        $record['chosenSubSubjects'][0]['name'];

        return $record;
    }

}