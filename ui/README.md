# Building the client

Use the [/ui/build](./build) script.

```bash
ui/build --help
```

Start it up in watch mode with `ui/build -w` to continuously rebuild when changes are detected. Keep an eye on stdout for build errors or new manifests. Manifests list public javascript and css assets that browsers must fetch independently. 

When changes compile successfully, a new manifest is created and your dev lila server will list those updated assets in any subsequent responses. Just reload your browser to see the results.

# Testing

The frontend uses the [Vitest](https://vitest.dev/) testing framework.

```bash
pnpm test
## or
pnpm test:watch
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
tsc type checking uses package.json properties to resolve static import declarations in external sources to the correct declaration (\*.d.ts) files in the imported package. When a single declaration barrel describes all package types, we can use the "types" property as shown in this example from [/ui/voice/package.json](./voice/package.json):

```json
  "types": "dist/voice.d.ts",
```

When more granular access to the imported package is needed, we use the "typesVersion" property to expose selective imports within the dist folder along with an optional "typings" property to identify a main barrel within the "typesVersion" mapping.

```json
  "typings": "common",
  "typesVersions": {
    "*": {
      "*": [
        "dist/*"
      ]
    }
  },
```

## esbuild import resolution
The [esbuild bundler](https://esbuild.github.io/getting-started/#your-first-bundle) does things a bit differently. It uses an "exports" object to resolve static workspace imports directly to the imported package's typescript source files. Declaration (*.d.ts) files are not used.

In this example from [/ui/opening/src/opening.ts](./opening/src/opening.ts):

```typescript
import { initMiniBoards } from 'lib/view/miniBoard';
import { requestIdleCallback } from 'lib';
```

The above 'lib/view/miniBoard' and 'lib' import declarations are mapped to the typescript sources by this snippet from [/ui/lib/package.json](./lib/package.json):

```json
  "exports": {
    ".": "./src/common.ts",
    "./*": "./src/*.ts"
  },
```
That maps `from 'lib'` to `src/common.ts` and `from 'lib/view/miniBoard'` to `src/miniBoard.ts`.

While esbuild may bundle imported code directly into the entry point module, it may also split imported code into "lib" chunk modules that are shared and imported by other workspace modules. This chunked approach is called code splitting and reduces the overall footprint of asset transfers over the wire and within the browser cache.

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
    "hash": { "glob": "<pattern>", "update": "<package-relative-path>" }
```
When the object form is processed, symlinks for globbed files are created in /public/hashed same as before. Then the "update" file is processed and all occurrence of those globbed filenames are replaced with their hashed symlink URLs. The modified "update" file contents are also content-hashed and written to /public/hashed. This is useful when an asset references other files by name and those references must be updated to reflect the hashed URLs. Any asset mapping within a static json or text file can be kept current in this way.

* "hash" sources must begin with `/public` to resolve correctly on production deployments.
* "update" files may not begin with `/` and are always package relative.

The node sources for ui/build are in the [/ui/.build](./.build) folder.
