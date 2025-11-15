# Building the client

Use the [/ui/build](./build) script.

```bash
ui/build --help
```

Start it up in watch mode with `ui/build -w` to continuously rebuild when changes are detected.

When changes compile successfully, stdout will report creation of a new manifest. Manifests list public javascript and css assets that browsers must fetch independently. The server communicates those updated URLs in subsequent responses. Just reload your browser to see the results.

# Testing

Use the [/ui/test](./.test/runner.mjs) script. It's a simple wrapper for node's test runner.

```bash
ui/test            # build ui/*/tests/**/*.ts

ui/test -w         # watch ui/*/tests/**/*.ts

ui/test winning    # ui/lib/tests/winningChances.test.ts

ui/test mod once   # ui/mod/tests/**/*.ts ui/lib/tests/once.test.ts
```

# About packages

Our client source code is arranged as a pnpm monorepo workspace described by [/pnpm-workspace.yaml](../pnpm-workspace.yaml). Each individual workspace package in the /ui/ folder has a package.json file that describes source files, dependencies, and build steps.

One workspace package (such as "analyse") may depend on another (such as "lib") using the standard "dependencies" property keyed by the package name "lib" but with "workspace:\*" as the version. 

```json
  "dependencies": {
    "lib": "workspace:*"
  }
```
That tells [pnpm](https://pnpm.io) and our build script to resolve the dependency with [/pnpm-workspace.yaml](../pnpm-workspace.yaml).

We do not use devDependencies because no package artifacts are published to npm. There is no useful distinction between dependencies and devDependencies when we're always building assets for the lila server.

## tsc import resolution
tsc type checking uses package.json's `exports` property [(node)](https://nodejs.org/api/packages.html#packages_exports) to resolve static import declarations in external sources to the correct declaration (\*.d.ts) files in the imported package.

```json
  "exports": {
    ".": {
      "types": "./dist/index.d.ts"
      "import": {
        "source": "./src/index.ts"
        "default": "./dist/index.js"
      }
    }
  }
```
tsc needs both `types` and `import` -> `default` to point to .d.ts and .js products during the typechecking (--noEmit) phase. tsc does not care about "source"

The `exports` object [(typescript)](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-7.html#packagejson-exports-imports-and-self-referencing) allows per directory remaps for barrel exports. With the following [/ui/lib/package.json](./ui/lib/package.json):
```json
  "exports": {

    ...

    "./ceval": {
      "types": "./dist/ceval/index.d.ts"
      "import": {
        "source": "./src/ceval/index.ts",
        "default": "./dist/ceval/index.js"
      }
    }
  }
```
An external package can import from the lib/ceval/index.ts barrel with:

```typescript
import { type X, Y, Z } from 'lib/ceval';
```

## esbuild import resolution
The [esbuild bundler](https://esbuild.github.io/getting-started/#your-first-bundle) uses `exports` as well but ignores "types", *.d.ts, and *.js files. It consumes only the `source` value within an `import` property. The value must be package relative to the typescript source(s).

```json
  "exports": {
    "./boo/*": {
      "import": {
        "source": "./src/boo/*.ts"
      }
    }
  }
```
##### Note - While esbuild may bundle imported code directly into an entry point module, it may also split imported code into "lib" chunk modules that are shared and imported by other workspace modules. This chunked approach is called code splitting and reduces the overall footprint of asset transfers over the wire and within cache.

## "build" property (top-level in package.json)

We define a custom "build" property object to describe how [/ui/build](./build) generates assets for the website.

Properties within build come in three flavors - "bundle", "sync", and "hash". Each of these can have one or more entries containing pathnames or globs. 

Usually, anywhere a pathname can be given, you can use a [micromatch glob](https://github.com/micromatch/micromatch) instead. The following resolution rules always apply to pathnames and globs (except when explicitly stated):
- "bundle" paths are resolved relative to the package folder
- "sync" paths that start with `/` resolve to the git repo root. Otherwise, they resolve relative to the package folder.
- "hash" paths are resolved same as "sync", with `/` mapping to repo root otherwise package relative.

## "bundle" property
The "bundle" property defines the javascript modules to create as named entry points which can be fetched by client browsers. Use a glob pattern to match multiple module sources and bundle each into named javascript entry points.

This excerpt from [/ui/analyse/package.json](./analyse/package.json) matches analyse.ts, analyse.nvui.ts, analyse.study.ts, analyse.study.topic.form.ts, and analyse.study.tour.ts from various places in the folder hierarchy within analyse/src:

```json
  "build": {
    "bundle": "src/**/analyse.*ts"
  }
```

Bundling results in one or more es6 modules flattened into the /public/compiled folder. Filenames are composed with the module source basename, followed by an 8 char content hash, and ending with .js. The directory components of the input pathname are ignored, so:

* **We have a rule** - source filenames of entry points must be prefixed by their package followed by a decimal.

Bundles may also be described by objects containing a "module" path and an "inline" path. 

* Globs are not allowed for either path when an "inline" property is given in object form.

The "module" path serves the same purpose as a bare string - naming the source module for an entry point. The "inline" path identifies a special typescript source from which ui/build will emit javascript statements into a special manifest.\*.json entry.

When that module is requested by a browser, the lila server injects those inline statements into a \<script> tag following the assembled DOM within the \<body> element. This allows blocking setup code to manipulate the DOM based on viewport calculations before rendering to avoid FOUC. This should be rare and globs are not supported here. [/ui/site/package.json](./site/package.json) shows an example:

```json
    "bundle": [
      "src/site.*Embed.ts",
      {
        "module": "src/site.ts",
        "inline": "src/site.inline.ts"
      }
    ],
```

## "sync" property

The sync object describes filesystem copies performed by ui/build. Sync operations are listed as properties where each key is a source path/glob and its value is a destination folder. In watch mode, ui/build will copy assets to the destination folder whenever they change.

One usage for sync is to copy npm package assets from node_modules to the /public/npm folder where they can be fetched and imported dynamically, often because they are too large to bundle. This example from [/ui/lib/package.json](./lib/package.json) copies assorted stockfish wasms to /public/npm:

```json
    "sync": {
      "node_modules/*stockfish*/*.{js,wasm}": "/public/npm"
    },
```

Sync watch is helpful when you must link a [local version of an npm package](https://github.com/lichess-org/lila/wiki/Lichess-UI-Development#customizing-linked-pnpm-modules). Issues involving chessground, pgn-viewer, or third party dependencies often require this.

## "hash" property

Why hash? Web asset distribution involves frequent caching, and hashes provide a repeatable way to compute URLs as a function of a file's content. ui/build writes the hashes used to cache asset URLs to a manifest.\*.json file. The server uses this manifest to link content-hashed URLs for unique asset versions in every response. Between the client and server, our Content Delivery Network (CDN) caches static assets via edge servers located around the world. Once the first request for a unique URL triggers an initial response from the lichess server, subsequent requests for that same URL from that region do not involve the lichess data center. The CDN edge servers persist cache entries for up to a year, so responses for the same URL are frozen in time without manual intervention.

ui/build automatically hashes compiled and bundled sources such as typescript and scss. For unmanaged assets, like images, fonts, and dynamically loaded npm repo files, you must use "hash" entries.

Hash entries identify files for which a symlink named with their content hash will be created within /public/hashed. The symlink points back to the original file, ensuring that when the file's content changes on the filesystem, its corresponding symlink gets a new name. Asset URLs use the symlink rather than the base filename, allowing optimal CDN caching and distribution.

"build" / "hash" may contain a single entry or an array of entries. Entries may be bare string globs:

```json
    "hash": [
      "/public/lifat/background/montage*.webp",
      "/public/npm/*",
      "/public/javascripts/**",
    ]
```
Entries may also take object form:
```json
    "hash": { "path": "<pattern>", "omit": true, "catalog": "<path-to-catalog>" }
```
When the object form is processed, symlinks for files globbed by the "path" pattern are created in /public/hashed same as before.

Setting the optional "omit" field to true will omit all "path" globbed items from the client manifest. They will stil appear in the server manifest.

The optional "catalog" field may identifies a mapping file to be transformed. All occurrences of filenames globbed by the "path" pattern within the catalog file are replaced with their hashed symlink URLs. The modified catalog file contents are also content-hashed and written to /public/hashed. This is useful when an asset references other files by name and those references must be updated to reflect the hashed URLs. Any asset mapping within a static json or text file can be kept current in this way.

* hash paths must begin with `/public` to resolve correctly on production deployments.

The node sources for ui/build are in the [/ui/.build](./.build) folder.
