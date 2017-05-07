<?php
namespace DeepTest;

class UnitTest /** extends \PHPUnit_Framework_Suite */
{
    public function provideIoPairs()
    {
        $list = [];

        // from function
        $list[] = [
            TestData::makeStudentRecord(),
            ['id' => [], 'firstName' => [], 'lastName' => [], 'year' => [], 'faculty' => [], 'chosenSubSubjects' => []],
        ];

        // from var
        $denya = TestData::makeStudentRecord();
        $list[] = [
            $denya,
            ['id' => [], 'firstName' => [], 'lastName' => [], 'year' => [], 'faculty' => [], 'chosenSubSubjects' => []],
        ];

        // from inner key
        $list[] = [
            $denya['friends'][0],
            ['name' => [], 'occupation' => []],
        ];

        return $list;
    }

    /**
     * @test
     * @dataProvider provideIoPairs
     */
    public function testCase($input, $expectedOutput)
    {
        $actualOutput = $input;
        /** self::assertArraySubset($expectedOutput, $actualOutput) */
    }
}