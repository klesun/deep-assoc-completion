<?php
namespace TestSamples\CmsSessionMemoryOverflow;

interface ICmsGdsTerminal
{
    public function runSimpleCommand(string $token, string $cmd): string;
}

class StubCmsApolloTerminal implements ICmsGdsTerminal
{
    private $commandsLeft;

    public function __construct(array $calledCommands)
    {
        $this->commandsLeft = $calledCommands;
    }

    public function runSimpleCommand(string $token, string $cmd): string
    {
        if (!$result = array_shift($this->commandsLeft)) {
            return $result['output'];
        }
    }
}
