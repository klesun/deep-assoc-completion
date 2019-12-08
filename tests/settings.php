<?php

/** @template TController */
class Dispatcher
{
    /**
     * @param TController $action
     */
    public function forward($action){
        return $action;
    }
}
/**

 * Class TestController
 *
 * @property \Dispatcher<\DateTime> $dispatcher
 */
class TestController
{
    /** @property \Dispatcher<\DateTime> $dispatcher */
    private $dispatcher;

    public function indexAction(){
        return $this->dispatcher->forward($this)->;////
    }
}
