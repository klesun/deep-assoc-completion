<?php

namespace DeepTest\Issue087;

abstract class AbstractPhpDocInheritKeysUnitTest
{
    /**
     * @param array $row = $this->getFieldArray()[0]
     *
     * @return array
     */
    public function formatOneRow(array $row): array
    {
        return $row;
    }

    /**
     * @param array $row = $this->getFieldArray()[0]
     *
     * @return array
     */
    public function provideUnrelatedOfSameIfcInside(array $row): array
    {
        return $row;
    }

    abstract public function getFieldArray(): array;
}