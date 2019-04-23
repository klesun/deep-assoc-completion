<?php
namespace PHPSTORM_META {
    expectedReturnValues(\Library\Book::getStruct(), [
        'title' => 'The C Language',
        'author' => 'Kernigan & Ritchie',
        'chapters' => [['name' => 'Abstract', 'content' => 'Abababababa hello world abababababa']],
    ]);
}


