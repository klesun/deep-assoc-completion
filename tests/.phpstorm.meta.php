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

    expectedArguments(\Library\IProduct::purchase(), 0, [
        'ccVendor' => 'visa',
        'ccNumber' => '411111111111111',
        'expirationMonth' => 12,
        'expirationYear' => 2021,
    ]);
    expectedReturnValues(\Library\IProduct::purchase(), [
        'status' => 'success',
        'confirmationCode' => 'ab34225',
        'transactionDt' => '201-04-15 22:33:44',
        'serialKey' => 'anetb-3ghdg5-fdsf4-sdfgre3-h45f3f',
    ]);

    expectedArguments(\Library\InfoHolder::read(), 0, [
        'chunkSize' => 20,
        'timeoutMs' => 20 * 1000, // 20 seconds
        'errorHandler' => function($code, $msg){},
    ]);
    expectedReturnValues(\Library\InfoHolder::read(), [
        'connection' => 0x9235256,
        'bytesChunk' => [123, 76,37, 256, 747],
        'bytesLeft' => 59198584,
    ]);
}


function gzip_by_dict($params)
{
}
