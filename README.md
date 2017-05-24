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
