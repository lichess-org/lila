## css variables vs scss variables

- scss variables start with $ (dollar sign) and are compile-time macros. when the css is
  generated, they're replaced by values and no longer exist.
- css variables begin with -- (two hyphens) and exist at runtime. they are set and accessed by
  the browser when it applies styles and can also be set and accessed by javascript.

## how color themes work

each partial scss file in the `ui/common/css/theme` directory describes a color theme but
`_default.scss` does double duty. in addition to defining the dark theme, it provides
important definitions to other named themes such as `_light.scss` and `_transp.scss`.
some (or all) of the included colors may be overridden by the named theme.

all of your external style rules will still reference colors as scss variables. under the
hood, they're generated wrappers defined in theme/gen/\_wrap.scss that look something like:

```scss
  $c-bg: var(--c-bg);
  $c-primary: var(--c-primary);
  ...
  $c_bad_bg-zebra--mix-20: var(--c_bad_bg-zebra--mix-20);
  $c_bad_bg-zebra2--mix-20: var(--c_bad_bg-zebra2--mix-20);
  ... // and so on
```

## when to use scss vs css variables in these theme files

### scss variables

these define the base colors of a theme.

currently, the values must be valid css color definitions (hsla, rgba, hex) and may not
contain scss color functions. however, when the build process encounters a variable name
following the pattern

```scss
$c_<color-1>_<optional-color-2>--<operation>-<val>
```

it performs
a mutation on the color(s) just like an explicit scss function might. this results in a new
css/scss variable pair in `gen/_mix.scss` and `gen/_wrap.scss` respectively. you don't
have to do anything special to make this happen aside from following the special syntax
above within a style rule.

for example, say we have $c-primary and $c-bg-zebra defined in a theme file. if ui/build
encounters

```scss
background: $c_primary_bg-zebra--mix-40;
```

then the background will be set to
a 40% mix of `$c-primary` with `$c-bg-zebra`.

supported operations are `fade`, `mix`, `alpha`, and `lighten`. `val` is always between
0 and 100 (where 100 represents either 100% or 1.0 depending on the function). other
functions `saturate`, `desaturate` etc can be added to `ui/.build/src/sass.ts` if needed.

### css variables

these are not mixable at compile time. but we need them for all colors save ones that are
auto-created by one of the mix operations described above.
most of these css variables are defined by the `shared-color-defs` mixin in `_default.scss`.
this mixin uses the scss variables of the including scope to inform most of the values.
others, such as `--c-page-mask` are the same for all current themes and are just given
an explicit value, but anything given a value in shared-color-defs can also be
overridden by the including theme file. that's why the light and transp themes include
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
  // `background: $c_font_bg--mix-50;` will give a purple background
}
```

## how do i run this crap?

same as before

```
ui/build -r
```

watch mode should keep everything in sync for you, but you might need to restart if
you create a brand new file somewhere
