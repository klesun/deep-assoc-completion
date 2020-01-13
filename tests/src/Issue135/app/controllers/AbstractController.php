<?php
// app/controllers/AbstractController.php
namespace App\Controllers;

abstract class AbstractController
{
    /** @var Container */
    protected $container;

    /**
     * AbstractController constructor.
     * @param Container $container = \Slim\App::getContainer()
     */
    public function __construct(Container $container)
    {
        $this->container = $container;
        $this->container['settings'][''];
        // it sees $this->container['settings'] which are empty
    }
}
