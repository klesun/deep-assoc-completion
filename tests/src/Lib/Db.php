<?php
namespace Lib;

use \Lib\Diag;
use Lib\Utils\Fp;

class Db
{
    /** @var \PDO */
    private $connection = null;

    protected function __construct()
    {
        $this->makeConnection();
    }

    public static function inst()
    {
        return new self();
    }

    protected function makeConnection()
    {
        try {
            list($host, $port) = array_pad(explode(':', Config::get('database', 'host')), 2, null);
            $dbname = Config::get('database', 'dbname');
            $user = Config::get('database', 'user');
            $pass = Config::get('database', 'password');

            $useEmulatePrepares = Config::getOptional('database', 'use_emulate_prepares');
            $useEmulatePrepares = ($useEmulatePrepares === null) ? true : $useEmulatePrepares;

            $db = new \PDO("mysql:host=$host;".($port ? "port=$port;" : '')."dbname=$dbname;charset=utf8", $user, $pass, [\PDO::ATTR_PERSISTENT => false]);
            $db->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
            $db->setAttribute(\PDO::ATTR_DEFAULT_FETCH_MODE, \PDO::FETCH_ASSOC);
            $db->setAttribute(\PDO::ATTR_EMULATE_PREPARES, $useEmulatePrepares);

            $this->connection = $db;
        } catch (\PDOException $e) {
            throw $e;
        }
    }

    public function exec($sql, $params = null)
    {
        if ($params === null) {
            return $this->connection->exec($sql);
        } else {
            return $this->connection->prepare($sql)->execute($params);
        }
    }

    public function fetchAll($sql, $params = null)
    {
        try {
            if ($params === null) {
                $stmt = $this->connection->query($sql);
            } else {
                $stmt = $this->connection->prepare($sql);
                $stmt->execute($params);
            }

            $result = [];
            while ($row = $stmt->fetch(\PDO::FETCH_ASSOC)) {
                $result[] = $row;
            }
            $stmt->closeCursor();
            return $result;
        } catch (\Exception $ex) {
            throw $ex;
        }
    }
}
