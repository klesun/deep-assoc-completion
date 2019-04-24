<?php

namespace Library {
    abstract class InfoHolder {
        public $capacity;
        public abstract function read($params);
    }

    interface IProduct {
        public function purchase($paymentData);
    }

    class Book extends InfoHolder implements IProduct {
        public function getStruct() {}
        public static function search($source, $filters) {}
        public function purchase($paymentData) {}
        public function read($params) {}
    }

    (new \Library\Book())->purchase([])[''];
    $read = (new \Library\Book())->read([
        ''
    ]);








    $pages = (new \Library\Book())->getStruct()[''];
    $results = Book::search('goodreads.com', [
        '' => ['horror'],
    ]);

}
