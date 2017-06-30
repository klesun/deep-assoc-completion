Autocomplete keys of associative arrays defined in other functions.

![alt tag](/imgs/screenshot.png)

Precompiled `.jar` (may be outdated): [mirror1](http://midiana.lv/Dropbox/web/phpstorm-deep-keys.jar), [mirror2](https://www.dropbox.com/s/5x984zqxw1u32fl/phpstorm-deep-keys.jar?dl=0).

Steps to compile plugin into a `.jar` follow:
- Start creating a new project in _[Intelliji Idea](https://www.jetbrains.com/idea/)_.
- Select `Intelliji Platform Plugin`.
- Select a _phpstorm_ installation directory as `Project SDK` (java version is 8).
- Select phpstorm-deep-keys project folder as `Project location`.
- In `Project Structure -> Libraries` add `php.jar` and `php-openapi.jar` from `YourPhpStormDirectory/plugins/php/lib/`.
- In `Project Structure -> Modules -> Dependencies` set `Scope` of `php-openapi` to `Provided`.

To build a jar use `Build -> Prepare Plugin ... For Deployment`. To debug use `Run -> Debug`. Since phpstorm project takes about a minute to start, you must find `Run -> Reload Changed Classes` very useful for micro changes.

To use compiled `.jar` in your phpstorm go to `Settings -> Plugins -> Install plugin from disk` and select the `.jar` we compiled earlier.

<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<hr/>
<hr/>

## Features description

### Completion from expression
When you are going to type an associative key of a variable, like in `$user['']`, put caret between quotes and press `ctrl` + `space`. The plugin will analyze your code, determine what keys does `$user` have and suggest completion. It should become pretty intuitive when plugin can determine keys, and when it can't. It still has some unsupported completion sources - i'm working on them.

The suggested completion may clash with phpstorm's built-in completion. In such case plugin keys will always be at the bottom and in **bold** - hit `Page Down` several times to get to them.

### Go To Definition
![Go To Definition](https://cloud.githubusercontent.com/assets/5202330/26428215/284b1988-40e9-11e7-9a44-746145c5393f.png)

To go to the key definition, hover on it and press `ctrl` + `click` or put carret on it and press `ctrl` + `b`.

### Completion from phpdoc
![Completion from phpdoc](https://cloud.githubusercontent.com/assets/5202330/26426602/0f72f554-40e2-11e7-8873-30b873310746.png)

You can specify function argument type using `@param {optionalType}? $varName = {expression}`, like `@param $anime = ['genre' => 'shounen', 'studio' => 'Shaft']`. `=` is mandatory and expression must be a valid php expression. Class methods can be specified either with complete namespace like `\Very\Long\Namespace\ClassName::funcName()`, or with just `ClassName::funcName()`.

### Describe variable
![Describe variable](https://cloud.githubusercontent.com/assets/5202330/26427776/ee6d4e54-40e6-11e7-83d5-81a1687a0d7a.png)

To get variable/expression result structure as lousy json, put caret on it and press `ctlr` + `alt` + `q`.
