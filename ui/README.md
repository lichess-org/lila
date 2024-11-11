# Client-side modules

## Building

Client builds are performed by the [ui/build](./.build/readme) script. Stick to `ui/build -wc` and leave it running. This does a clean build then rebuilds any client source files when changed. A browser reload will show the results.

```bash
ui/build --help
```

## Testing

The frontend uses the [Vitest](https://vitest.dev/) testing framework.

```bash
pnpm test
## or
pnpm test:watch
```

## CSS

The structure of a CSS module is as follows:

```
- css/
  - forum/
    - \_forum.scss # imports the files below
    - \_post.scss
    - \_search.scss
    - ...
  - build/
    - \_forum.scss # imports dependencies and `../forum/forum`.
    - forum.light.scss # generated
    - forum.dark.scss # generated
    - forum.transp.scss # generated
```
