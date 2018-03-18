<?php
namespace Illuminate\Database\Eloquent;

/**
 * adding here a fake version of Laravel's model in same namespace to test
 * the property array completion when you instantiate a class extending it
 */
abstract class Model
{
    /**
     * The attributes that are mass assignable.
     *
     * @var array
     */
    protected $fillable = [];

    /**
     * The attributes that aren't mass assignable.
     *
     * @var array
     */
    protected $guarded = ['*'];

    /**
     * Indicates if all mass assignment is enabled.
     *
     * @var bool
     */
    protected static $unguarded = false;

    public function __construct(array $attributes = [])
    {
        // ... do some internal stuff ...
    }
}
