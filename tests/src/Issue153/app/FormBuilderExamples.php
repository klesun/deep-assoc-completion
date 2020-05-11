<?php
namespace App;
class FormBuilderExamples {
    public static function options() {
        return [
            'redirect'             => '',
            'permission'           => '',
            'form_view'            => 'streams::form/form',
            'wrapper_view'         => '',
            'layout_view'          => '',
            'breadcrumb'           => '',
            // and a lot more, not needed for this example..
        ];
    }
}
