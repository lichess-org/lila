# Client-side modules

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

## Building

````
Client builds are currently performed by the ui/build script.  Usage examples:

  build                     # builds all client assets in dev mode (inline sources)
  build -w                  # builds all client assets and watches for changes
  build -p                  # builds minified client assets (prod builds)
  build analyse site msg    # specify modules (don't build everything)
  build -w dasher chart     # watch mode for specific modules
  build --tsc -w            # watch mode but type checking only
  build --sass msg notify   # build css only for msg and notify modules
  build --esbuild           # run esbuild only
  build --no-color          # don't use color in logs
  build --no-time           # don't log the time
  build --no-context        # don't log the context ([sass], [esbuild], etc)```

In most cases, just stick to ui/build -w. This incrementally rebuilds the sass for all ui/ modules on file change.

### Hack

The structure of a CSS module is as follows:

````

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

```
