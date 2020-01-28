<?php
class Asd {
    public function ololo() {

    }
}
call_user_func([new Asd(), "doesNotExist"]);
$reference = [new Asd(), "ololo"];

