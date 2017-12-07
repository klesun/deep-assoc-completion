<?php
namespace TestSamples\CmsSessionMemoryOverflow;

interface ITerminalCommandLog
{
    public function getSessionData(): array;
}

class InstanceCmdLog implements ITerminalCommandLog
{
    private $sessionData;
    private $letterToArea = [];

    public function __construct(array $cmdRows, array $sessionData)
    {
        $this->sessionData = $sessionData;
    }

    private function getAreaData(string $letter)
    {
        return $this->letterToArea[$letter];
    }

    public function logCommand(array $calledCommand)
    {
        $getAreaData = function($letter){return $this->getAreaData($letter);};
        $this->sessionData = SessionStateProcessor::updateStateSafe(
            $calledCommand['cmd'], $calledCommand['output'], $this->sessionData, $getAreaData
        );
        $this->letterToArea[$this->sessionData['area']] = $this->sessionData;
    }

    /** @return array like Db::fetchOne('SELECT * FROM terminal_sessions') */
    public function getSessionData(): array
    {
        return $this->sessionData;
    }
}
