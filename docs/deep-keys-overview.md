Hello, in this document I'd like to show you the features of the "deep-assoc-completion" phpstorm plugin I wrote.

The plugin adds associative array key completion for you. So when you declare a variable like that...

```php
$anime = [
    'name' => 'Bleach',
    'score' => 7.5,
    'genre' => 'shounen',
];
```

and then access a key on this variable by typing:

```php
$anime['']; // should suggest: "name", "score", "genre"
```

the plugin suggests you the keys this array has. Well, that alone is not such a great deal, even phpstorm itself, as you see, has somewhat similar built-in functionality. 

But what if your array was actually created in a different function, usually even in a different file, and you assign result of it to your variable ?

```php
function getAnimeList()
{
    return [
        ['name' => 'Bleach', 'score' => 7.5, 'genre' => 'shounen'],
        ['name' => 'Cardcaptor Sakura', 'score' => 7.6, 'genre' => 'mahou shoujo'],
        ['name' => 'Higurashi', 'score' => 7.8, 'genre' => 'mystery'],
    ];
}

function getMostPopularAnime()
{
    return array_pop(getAnimeList());
}

$anime = getMostPopularAnime();
$anime['']; // should suggest: "name", "score", "genre"
```

How about that, phpstorm? Too complex for you, eh? 
So, what do we do, how do we know what keys does this variable have? We aren't supposed to dive deep into source code to collect the key names from everywhere and write them down are we?

Luckily, no, because, this situation is a piece of cake for the "deep-assoc-completion" plugin. So, you just install it through _File -> Settings -> Plugins -> Browse Repositories -> deep-asoc-completion_, restart phpstorm, and voile, here is your completion. The plugin looks really deep into the origins of the contents of this variable to get the type info - 40 expressions by default, enough to cover nested arrays of about 10 levels depth at once:

```php
// example from a unit test
$policeDepartment['offices']['402']['deskByTheWindow']['dayShift']['cases']['8469132']['evidences'][0]['value'];
```
(the depth is configurable in _File -> Settings -> Languages & Frameworks -> PHP -> deep-asoc-completion_)

You can go to the definition of a key, by Ctrl+Pressing on it, or putting carret at it and pressing Ctrl+B (or corresponding key on Mac if any), just like how you would get to the definition of a function or a field.

The completion works with `foreach`, with `list(...) = ...` assignment, with most built-in functions, like `array_map`, `array_merge`, `array_combine`, etc... and even with methods passed by reference like `[SomeClass::class, 'someMethod']`.

If you find a case when completion does not work when it should, you may file an issue in github.

___________________________________________________________________

Another important feature this plugin provides, without which it would be of little use, is the ability the specify the keys of function argument in phpdoc using `@param` tag:

```php
/**
 * @param $employee = [
 *     'salary' => 800.00,
 *     'position' => 'frontend developer',
 *     'fullName' => 'Pupkin Vasja',
 * ]
 */
function promote($employee)
{
    $employee['']; // should suggest: "salary", "position", "fullName"
    $employee['position'] = 'senior '.$employee['position'];
    $employee['salary'] += 200.00;
}
```

The part after the `=` in the doc should be a valid php expression (or at least something that jetbrains's parser will chew up).

I usually use some common example values for the keys in the doc, that helps reader to get the grasp of what sort of data it is. I showed this way of documenting to you just for reference, you likely are not going to use it much, because plugin also provides another, more practical way to specify type in the doc:

```php
namespace Org\Klesun\HiringSystem;

class EmployeeUtil
{
    public static function getEmployee(int $id)
    {
        $cols = ['salary', 'position', 'fullName'];
        $row = Db::fetchRow('employee', ['id' => $id], $cols);
        list($salary, $position, $fullName) = $row;
        return [
            'salary' => $salary,
            'position' => $position,
            'fullName' => $fullName,
        ];
    }
    
    /** @param $employee = EmployeeUtil::getEmployee() */
    public static function promote($employee)
    {
        $employee['']; // should suggest: "salary", "position", "fullName"
        $employee['position'] = 'senior '.$employee['position'];
        $employee['salary'] += 200.00;
    }
}
```

So you can specify the type of variable using the name of the function it was created in, and that is something i use a lot, because it's super cool. You can either specify full namespace, or just the class name - in that case if you have many classes with such name, you will get completion from _all_ of them. 

Plugin also adds the nice jetbrain's fuzzy completion of class/method name when you type the doc.

As you see, this doc is still a valid php expression, so you can do whatever you like with it - you can do this:

```php
/**  @param $employeeList = [EmployeeUtil::getEmployee(), ...] */
function findCheapestEmployee($employeeList)
{
    // "salary" in the `array_column` was sugested
    $bySalary = array_combine(array_column(employeeList, 'salary'), $employeeList);
    ksort($bySalary);
    $cheapest = array_shift($bySalary);
    $cheapest['cheapestSince'] = date('Y-m-d H:i:s');
    return $cheapest;
}
```

Or even this:

```php
/**  @param $cheapest = EmployeeUtil::findCheapestEmployee()[0] */
function motivate($cheapest)
{
    // key "cheapestSince" was suggested
    if (strtotime($cheapest['cheapestSince']) < strtotime('-365 days')) {
        $cheapest['salary'] += 5.00;
    }
    return $cheapest;
}
```

___________________________________________

Another cool feature i wrote just few days ago is completion in the array initialisation based on keys that are used in a function it is passed to. Imagine such situation:

```php
function makeSoapPayload($params)
{
    $products = $params['products'];
    $currency = $params['currency'];
    $amount = $params['amount'];
    return Xml::xmlify([
        'Security' => ['MarketingId' => 'ATN57NG3G'],
        'RequestTag' => [
            'Purchase' => [
                'Products' => ['Product' => $products],
                'AmountCharged' => [
                    'Currency' => $currency,
                    'Amount' => $amount,
                ],
            ],
        ],
    ]);
}

function purchaseViaSoap($products, $currency, $amount)
{
    BalanceValidator::take($currency, $amount);
    $payload = makeSoapPayload([
        'products' => $products,
        // should suggest: "products", "currency", "amount"
        '' => ,
    ]);
    return SoapUtil::perform('purchase', $payload);
}
```

The plugin suggests you the keys function uses. Is helps a lot sometimes.
_______________________________________________

for more detailed list of phpdoc formats, see:
https://github.com/klesun/deep-assoc-completion/issues/63
_______________________________________________

Last, but not... well, actually the least important feature - _Ctrl+Alt+Q_ - getting the json-like description of variable type. It may be handy sometimes when you, like, need to create an array with same keys as the variable, and you don't want to type them manually... And it helps to grasp the idea about the variable i guess.

![Describe variable](https://cloud.githubusercontent.com/assets/5202330/26427776/ee6d4e54-40e6-11e7-83d5-81a1687a0d7a.png)
