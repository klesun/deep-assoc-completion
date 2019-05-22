<?php

namespace DeepTest\Issue087\ClassFirstPhpDocInheritKeys;

use DeepTest\Issue087;

class ExactKeysUnitTest extends Issue087\AbstractPhpDocInheritKeysUnitTest
{
    /**
     * {@inheritdoc}
     */
    public function formatOneRow(array $row): array
    {
        $row['cmd_performed'] = strtoupper($row['cmd_performed']);

        return $row;
    }

    public function getFieldArray(): array
    {
        // this maybe comes from the database
        return [
            [
                'id' => 1,
                'cmd_performed' => 'apt-get update'
            ],
            [
                'id' => 2,
                'cmd_performed' => 'apt-get upgrade'
            ],
        ];
    }

    public function provideUnrelatedOfSameIfcInside(array $row): array
    {
        $row[''];
        return [
            [$row, ['id', 'cmd_performed']],
        ];
    }
}