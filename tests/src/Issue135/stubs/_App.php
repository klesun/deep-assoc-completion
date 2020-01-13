<?php
// stubs/_App.php
namespace Slim;

class App
{
    public function getContainer(): Container
    {
        $container = new Container();
        $container['settings'] = (require __DIR__ . '/../src/config.php')['settings'];

        return $container;
    }
}
