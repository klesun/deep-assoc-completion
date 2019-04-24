<?php
namespace PHPSTORM_META {
    expectedReturnValues(\Library\Book::getStruct(), [
        'title' => 'The C Language',
        'author' => 'Kernigan & Ritchie',
        'chapters' => [['name' => 'Abstract', 'content' => 'Abababababa hello world abababababa']],
    ]);
    expectedArguments(\Library\Book::search(), 1, [
        'id' => 1234,
        'minWords' => 100,
        'maxWords' => 900,
    ], [
        'pattern' => 'arnia',
        'genres' => ['fantasy', 'science fiction'],
    ]);
}


