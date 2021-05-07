# Client-side modules

## CSS

```
cd ui/
yarn install # only the first time
gulp css
```

This incrementally rebuilds the sass for all ui/ modules on file change.

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
