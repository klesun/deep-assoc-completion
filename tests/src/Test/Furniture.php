<?php

namespace Test;

class Furniture {

    /**
     * @param array $node
     *
     * @return self
     */
    public function add($node)
    {
        return $this;
    }
}

$class = (new Furniture())
    ->add(['chair' => ['',]])
    ->add(['sofa' => ['']])
    ->add(['window' => ['']])
    ->add(['table' => ['']]);

