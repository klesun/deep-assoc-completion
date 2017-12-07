<?php
namespace TestSamples\CmsSessionMemoryOverflow;

/**
 * updates state on each command and writes to DB
 *
 * probably storing calledCommands here would be appropriate
 * instead of taking them from DB every time we need them
 */
class CmsStatefulSession
{
    private $gdsInterface;
    private $terminalCommandLog;

    public function __construct(ICmsGdsTerminal $gdsInterface, ITerminalCommandLog $tcl)
    {
        $this->gdsInterface = $gdsInterface;
        $this->terminalCommandLog = $tcl;
    }

    private function hideSensitiveData(string $output)
    {
        if (rand() % 2) {
            $output = $output.'asd';
        }
        if (rand() % 2) {
            $output = $output.'dsa';
        }
        return $output;
    }

    // if you try to ask for completion from this function, idea will hang for several seconds
    // (initially it ran out of memory, but i simplified code to minimum)
    public function logAndRunCommandByGds(string $cmd, bool $fetchAll, bool $forceScrolling = false)
    {
        $output = $this->gdsInterface->runSimpleCommand($this->getSessionToken(), $cmd);
        $output = $this->hideSensitiveData($output);
        $calledCommand = [
            'cmd' => $cmd,
            'output' => $output,
            'dt' => date('Y-m-d H:i:s'),
        ];
        $calledCommand[''];
        return $calledCommand;
    }

    public function getSessionToken()
    {
        return $this->getSessionData()['internal_token'] ?? null;
    }

    public function getSessionData()
    {
        return $this->terminalCommandLog->getSessionData();
    }
}
