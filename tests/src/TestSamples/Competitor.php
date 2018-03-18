<?php

namespace App\Orm;

use Illuminate\Database\Eloquent\Model;

/**
 * @property int $id
 * @property \Carbon\Carbon $created_at
 * @property \Carbon\Carbon $updated_at
 * @property int $spice_left
 */
class Competitor extends Model
{
    protected $rounds_won;
    protected $spice_stolen;

    protected $guarded = [];
}
