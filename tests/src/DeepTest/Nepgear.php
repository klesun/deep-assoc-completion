<?php
namespace DeepTest;

trait TSetState
{
    public static function __set_state($an_array)
    {
        // TODO: Implement __set_state() method.
    }
}

class Nepgear
{
    use TSetState;

    public $bracer;
    public $pants;
    public $weapon;
    public $armor;

    // TODO: support completion when it is in a tratit/abstract class too
    public static function __set_state($an_array)
    {
        $self = new static();
        // both ways should provide completion
        foreach (get_object_vars($self) as $name => $value) {
            $self->$name = $an_array[$name] ?? null;
        }
        foreach ($an_array as $key => $value) {
            $self->$key = $value;
        }
        return $self;
    }
}