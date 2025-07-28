## css variables vs scss variables

- scss variables start with $ (dollar sign) and are compile-time macros. when the css is
  generated, they're replaced by values and no longer exist.
- css variables begin with -- (two or more hyphens) and exist at runtime. they are set
  and accessed by the browser when it applies styles and can also be set and accessed by javascript.

## how color themes work

each partial scss file in the `ui/lib/css/theme` directory describes a color theme.
`_theme.default.scss` is special as in addition to defining the dark theme, it is the baseline
from which other named themes (`_theme.light.scss`, `_theme.transp.scss`, ...) are extended.

every color in this theme system has both an scss and a css variable representation. your external
style rules will use the scss form beginning with `$c-` or `$m-` when possible due to
type safety reasons. under the hood, the build script generates import code that maps them.
something like:

```scss
  $c-bg: var(--c-bg);
  $c-primary: var(--c-primary);
  ...
  $m-bg_primary--mix-20: var(--m-bg_primary--mix-20);
  ... // and so on
```

### mixable scss variables defined in your external scss

outside of the theme files, when the build script encounters a variable name following the pattern

```scss
$m-<color-1>_<optional-color-2>--<operation>-<val>
```

it performs an operation (`fade`, `mix`, `alpha`, or `lighten`) on mixable theme color(s)
and a value (all within the same variable name) just like the equivalent scss color function would.
behind the scenes, this results in a new css/scss variable pair being created but
you don't have to do anything special aside from using the `$m-<color(s)>--<op>-<val>` syntax within a style rule.

for example, say we have $c-primary and $c-bg-zebra defined in a theme file. if ui/build is then
parsing ui/yourModule/css/_yourModule.scss and encounters

```scss
background: $m-primary_bg-zebra--mix-40;
```

then the background will be set to a 40% mix of `$c-primary` with `$c-bg-zebra`.

supported operations are `fade`, `mix`, `alpha`, and `lighten`. `val` is always between
0 and 100 (where 100 represents either 100% or 1.0 depending on the function).

### scss color variables defined in theme files

these are always prefixed by `$c-` define the base colors of a theme. only these colors may be used
in mix expressions.

the values for all scss variables atop the color theme files must be valid css color
definitions (hsla, rgba, hex) and may not contain scss color functions or other scss variables.

### css color variables defined on the html element in theme files

these are not mixable, but their definition rules are less strict - they
may contain any scss expression that resolves to a color (including scss color functions and variables).
many are defined by the `shared-color-defs` mixin in `_default.scss`.
this mixin uses the scss variables of the including scope to inform most of the values.
just like scss color variables, html block css variables can be
overridden by subsequent properties. that's why the light and transp themes include
the `shared-color-defs` mixin first thing in their selector block, so they can then
override the values they want to change.

## too long didn't read. give me an example

here's a complete theme file:

```scss
// ugly theme
@import 'default'; // for default defs and shared-color-defs mixin

$c-font: #f00; // these are your tentpole colors
$c-bg: #00f;
$c-primary: #0f0; // anything you don't specify here will be inherited from default

html.ugly {
  // the 'ugly' class is added to the html element by the theme switcher
  @include shared-color-defs;

  --c-bg-variation: #f0f;
  --c-bg-header-dropdown: #ff0;
  --c-border: #0ff;

  @include ugly-mix;
  // ugly-mix is a mixin created by ui/build that contains color mixes specified
  // elsewhere in your scss. these reflect the scss variable values above, so
  // `background: $m-font_bg--mix-50;` will give a purple background
}
```

elsewhere in your scss you can refer to $m-font_bg--mix-50 and that color will be generated
from the $c-font and $c-bg colors you defined in the theme file.

## how do i run this crap?

same as before

```
ui/build -w
```

watch mode should keep everything in sync for you
