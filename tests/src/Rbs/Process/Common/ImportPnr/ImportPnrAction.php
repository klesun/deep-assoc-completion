<?php
namespace Rbs\Process\Common\ImportPnr;

class ImportPnrAction
{
    public static function makeByGds()
    {
        return new self();
    }

    private static function getFieldMap()
    {
        return [
            static::RESERVATION =>
                function(IGdsPnrFieldsProvider $provider) { return $provider->getReservation(); },
        ];
    }

    private function retrieveField(string $fieldName)
    {
        $action = static::getFieldMap()[$fieldName];
        return $action();
    }

    private function retrieveFields()
    {
        $fetched = $this->retrieveField($fieldName);
        $retrievalResult[$fieldName] = $fetched;
        return $retrievalResult;
    }

    public function execute()
    {
        $retrievalResult = $this->retrieveFields();
        return ['result' => ['pnrFields' => $retrievalResult]];
    }
}
