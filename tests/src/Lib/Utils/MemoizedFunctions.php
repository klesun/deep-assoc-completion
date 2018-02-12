<?php
namespace Lib\Utils;

use Lib\Db;

class MemoizedFunctions
{
    private static $ramCache = [];

    public static function cachedFunctionCall($functionName, callable $function, $args, $expirationTime)
    {
        if (Config::getOptional('app', 'disable_caching')) {
            return $function(...$args);
        }
        $cachePrefix = 'Memo_g6j3bhDVxgc8y_';

        $cacheName = $cachePrefix.serialize($functionName).'_'.md5(serialize($args));
        $cache = new \Cache_Lite(['fileLocking' => true, 'lifeTime' => $expirationTime]);
        $cached = $cache->get($cacheName);
        if ($cached) {
            return unserialize($cached);
        } else {
            $result = call_user_func_array($function, $args);
            $cache->save(serialize($result), $cacheName);
            return $result;
        }
    }

    /** @param string|array $functionName */
    public static function ramCachedFunctionCall($functionName, $function, $args)
    {
        $cachePrefix = '';

        $cacheName = $cachePrefix.serialize($functionName).'_'.md5(serialize($args));

        if (!array_key_exists($cacheName, self::$ramCache)) {
            self::$ramCache[$cacheName] = call_user_func_array($function, $args);
        }

        return self::$ramCache[$cacheName];
    }

    /**
     * @param callable $function = [YourClass::class, 'yourMethod']
     * the method must be public
     */
    public static function cachedBothWays(array $function, array $args, $expirationTime)
    {
        $getViaMemcache = function(...$args) use ($function,$expirationTime){
            return static::cachedFunctionCall($function, $function, $args, $expirationTime);
        };
        return static::ramCachedFunctionCall($function, $getViaMemcache, $args);
    }
}
