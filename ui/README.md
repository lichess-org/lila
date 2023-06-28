# Client-side modules

## Building

Client builds are performed by the ui/build script. Stick to `ui/build -w` and leave it running when you can. This automatically rebuilds any client source files when changed and lets you quickly see the results in your browser. NOTE - always use hard refresh (google it) or disable caching in the network tab of your browser inspector to pick up fresh changes.

Usage examples:

```
  ui/build                     # builds all client assets in dev mode
  ui/build -w                  # builds all client assets and watches for changes
  ui/build -p                  # builds minified client assets (prod builds)
  ui/build --no-install        # no pnpm install (to preserve local links you have set up)
  ui/build analyse site msg    # specify modules (don't build everything)
  ui/build -w dasher chart     # watch mode but only for given modules
  ui/build --tsc -w            # watch mode but type checking only
  ui/build --sass msg notify   # build css only for msg and notify modules
  ui/build --no-color          # don't use color in logs
  ui/build --no-time           # don't log the time
  ui/build --no-context        # don't log the context ([sass], [esbuild], etc)
```

## Testing

The frontend uses the Jest testing framework.

```
cd ui/
pnpm test
```

### VSCode

The Jest extension is your friend. You will want to configure the following in your `settings.json`:

```
"jest.rootPath": "ui",
"jest.jestCommandLine": "node_modules/.bin/jest --config jest.config.js",
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
