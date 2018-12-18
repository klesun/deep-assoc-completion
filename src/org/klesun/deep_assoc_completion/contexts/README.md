"contexts" refers to the _state_ passed alongside the expression to a resolver which may
provide additional type info like `$this` or arguments passed to the function. It also
tracks and limits the resolution depth. _Context_ + _Expression_ = _Unique Resolved Type_

- `SearchCtx` is the root context. It tracks total resolved expressions limit, detects recursion, applies caching, etc...
- `FuncCtx` is the function call context. It holds the `$this`, `static::`, arguments passed to a function call and closure variables.
- `ExprCtx` is unique for every expression. It holds the parent expression, so it is possible to
    print resolution tree, apply depth limits even with lazy (iterator) resolution and detect recursion.