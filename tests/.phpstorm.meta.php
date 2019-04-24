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
    expectedReturnValues(\gzip_by_dict(), [
        'bytes' => [2, 53, 80, 125, 18, 84, 36],
        'newDict' => ['&%hgfe#' => 'The London is the captial of Great Britain'],
    ]);
    expectedArguments(\gzip_by_dict(), 0, [
        'text' => 'OLOO The London is the captial of Great Britain OLOLO',
        'oldDict' => ['&%hgfe#$SA' => 'The London is the captial of Great Britain'],
    ]);
}


function gzip_by_dict($params)
{
}
