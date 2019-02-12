# Client-side modules

## CSS

Say we're working on the `ui/site` module.
It contains code for all pages that don't have a dedicated module.
For instance, team, forum, mod pages are in ui/site.

`cd ui/site`

### Build

```
gulp css
```

This rebuilds the sass on change.

### Hack

The structure of a CSS module is as follows:

```
- css/
  - forum/
    - _forum.scss # imports the files below
    - _post.scss
    - _search.scss
    - ...
  - build/
    - _forum.scss       # imports dependencies and `../forum/forum`.
    - forum.light.scss  # generated
    - forum.dark.scss   # generated
    - forum.transp.scss # generated
```
