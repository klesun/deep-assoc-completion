<?php
class ArrMethRefIssue {
    public function ololo2() {}
    public static function ololo3() {}
}
call_user_func([new ArrMethRefIssue(), "doesNotExist"]);
