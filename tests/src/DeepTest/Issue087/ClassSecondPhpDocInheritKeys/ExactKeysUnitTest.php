<?php

namespace DeepTest\Issue087\ClassSecondPhpDocInheritKeys;

use DeepTest\Issue087;

class ExactKeysUnitTest extends Issue087\AbstractPhpDocInheritKeysUnitTest
{
    /**
     * {@inheritdoc}
     */
    public function formatOneRow(array $row): array
    {
        $row['performed'] = strtolower($row['performed']);

        return $row;
    }

    public function getFieldArray(): array
    {
        // this maybe comes from the database
        return [
            [
                'id' => 1,
                'performed' => 'apt-get update'
            ],
            [
                'id' => 2,
                'performed' => 'apt-get upgrade'
            ],
        ];
    }

    public function provideUnrelatedOfSameIfcInside(array $row): array
    {
        $row[''];
        return [
            [$row, ['id', 'performed']],
        ];
    }
}