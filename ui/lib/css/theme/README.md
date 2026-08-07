## css variables vs scss variables

- scss variables start with $ and are compile-time macros. when the css is
  compiled, they're replaced by their values and no longer exist.
- css variables begin with -- (two or more hyphens) and exist at runtime. they are set
  and accessed by the browser when it applies styles and can also be set and accessed by javascript.

## how color themes work

each partial scss file in the `ui/lib/css/theme` directory describes a color theme.
`_theme.default.scss` (the dark theme) is the foundation from which other named themes
(`_theme.light.scss`, `_theme.transp.scss`, ...) are extended.

themeable colors are defined in html class scopes in those `_theme.*.scss` files. for example:

```scss
html.example-theme {
  ...
  --c-color: hsl(0 80% 100%);
  --c-color-desaturated: hsl(from var(--c-color1) h calc(s - 20) l)
  ...
}
```

the build script will generate scss wrapper variables beginning with `$c-` for each of these css
variables. your style rules should use the scss forms when possible for type safety.

`ui/lib/css/theme/gen/_wrap.scss` (generated):
```scss
  $c-color: var(--c-color);
  $c-color-desaturated: var(--c-color-desaturated);
  ... // and so on
```

## deriving new colors

a good rule for deciding whether a new color variable should be introduced is "does the color i want
need a unique hue?"

if not, use css
[`color-mix`](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/color_value/color-mix), relative [`<color>`](https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Colors/Using_relative_colors)
syntax, and `calc` to derive your color at page render time in a theme-friendly way.

for example:

```scss
  color: color-mix(in oklab,$c-primary 30%, $c-font) // a mix of 30% var(--c-primary) and 70% var(--c-font)
  background-color: hsl(from $c-bg h s calc(l + 20)) // equivalent to scss lighten($c-bg, 20%)
```

## too long didn't read. give me an example

here's a full theme file that thibault would love to see in a new PR today:

`ui/lib/css/theme/_theme.ugly.scss`:
```scss
@import 'theme.default'; // for default defs and shared-color-defs mixin

html.ugly {
  // the 'ugly' class is added to the html element when ugly theme is active
  @include shared-color-defs;

  ---site-hue: 210deg; // site-hue is a fun variable to tweak in browser dev-tools

  --c-bg: #00b;
  --c-font: #d00;
  --c-bg-variation: #f0f;
  --c-bg-header-dropdown: #ff0;
  --c-border: #0ff;
}
```

the values provided here will override those in _theme.default.scss and, crucially, will be
fed into all colors derived from these variables.

## how do i run it?


```bash
ui/build -w
```

watch mode will keep the scss wrapper variables in sync
