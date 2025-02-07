# Building the client

Use the [/ui/build](./build) script.

```bash
ui/build --help
```

You can start up `ui/build -w` in watch mode to continuously rebuild when changes are detected. Keep an eye on stdout for build errors. On success, reload the browser page for the results.

# Testing

The frontend uses the [Vitest](https://vitest.dev/) testing framework. Please write tests. Trevlar likes tests.

```bash
pnpm test
## or
pnpm test:watch
```

# About packages

Our client source code is arranged as a pnpm monorepo workspace described by [/pnpm-workspace.yaml](../pnpm-workspace.yaml). Each individual workspace package in the /ui/ folder has a package.json file that describes source files, dependencies, and build steps.

One workspace package (such as /ui/analyse) may depend on another (such as /ui/common) using a "dependencies" property keyed by the package name with "workspace:\*" as the version. That tells [pnpm](https://pnpm.io) and our build script that the dependency should be resolved by [/pnpm-workspace.yaml](../pnpm-workspace.yaml) (spoiler - it's in ui/common).

```json
  "dependencies": {
    "common": "workspace:*"
  }
```

We do not use devDependencies because no package artifacts are published to npm. There is no useful distinction between dependencies and devDependencies when we're always building assets for the lila server.

## tsc import resolution
tsc type checking uses package.json properties to resolve static import declarations in external sources to the correct declaration (\*.d.ts) files in the imported package. When a single declaration barrel describes all package types, we use the "types" property as shown in this example from [/ui/voice/package.json](./voice/package.json):

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
The [esbuild bundler](https://esbuild.github.io/getting-started/#your-first-bundle) does things a bit differently. It uses an "exports" object to resolve static workspace imports from other packages directly to the imported package's typescript source files. Declaration (*.d.ts) files are not used.

In this example from [/ui/opening/src/opening.ts](./opening/src/opening.ts):

```typescript
import { initMiniBoards } from 'common/miniBoard';
import { requestIdleCallback } from 'common';
```

The above 'common' and 'common/miniBoard' import declarations are mapped to the correct typescript sources by this snippet from [/ui/common/package.json](./common/package.json):

```json
  "exports": {
    ".": {
      ".": "./src/common.ts",
      "./*": "./src/*.ts"
    }
  },
```

While esbuild may bundle imported code directly into the entry point module, it may also split imported code into "common" chunk modules that are shared and imported by other workspace modules. This chunked approach is called code splitting and reduces the overall footprint of asset transfers over the wire and within the browser cache.

## "build" property (top-level in package.json)

We define a custom "build" property object to describe how [/ui/build](./build) generates assets for the website.

Properties within build come in three flavors - "bundle", "sync", and "hash". Each of these can have one or more entries containing pathnames or globs. For bundles, pathnames are resolved relative to the package folder. For sync and hash objects, paths that start with `/` are resolved relative to the git repo root. Otherwise, they are resolved relative to the package.

## "bundle" property
The "bundle" property tells esbuild which javascript modules should be created as named entry points. Most correspond to a server controller and scala module found in [/app](../app) and [/modules](../modules) respectively. These usually generate the html DOM on which the javascript operates.

Path elements within bundle may specify a glob pattern to match multiple module sources and bundle each into named javascript entry points.

This excerpt from [/ui/analyse/package.json](./analyse/package.json) matches analyse.ts, analyse.nvui.ts, analyse.study.ts, analyse.study.topic.form.ts, and analyse.study.tour.ts from various places in the folder hierarchy within analyse/src:

```json
  "build": {
    "bundle": "src/**/analyse.*ts"
  }
```

Bundle outputs are produced by esbuild as javascript esm modules in the /public/compiled folder. Filenames are composed with the module source basename, followed by an 8 char content hash, and ending with .js.

Bundles may also be described by objects containing a "module" path and an "inline" path. The "module" path serves the same purpose as a bare string - naming the source module for an entry point. The "inline" path identifies a special typescript source from which ui/build will emit javascript statements into a manifest.\*.json entry for the server.

When that parent module is requested by a browser, the lila server injects those inline statements into a \<script> tag following the assembled DOM within the \<body> element. This allows blocking setup code to manipulate the DOM based on viewport calculations before rendering to avoid FOUC. This should be rare and globs are not supported here. [/ui/site/package.json](./site/package.json) shows an example:

```json
    "bundle": [
      "src/site.*Embed.ts",
      {
        "module": "src/site.ts",
        "inline": "src/site.inline.ts"
      }
    ],
```

Globs are not allowed for either path when using the "inline" property.

## "sync" property

The sync object describes filesystem copies performed by ui/build. Sync object properties map source asset globs (keys) to destination folders (values). In watch mode, ui/build will copy assets to the destination folder whenever they change.

One usage for sync is to copy elements from node_modules (an npm package dependency) to the /public/npm folder where they can be fetched and imported dynamically, often because they are too large to bundle. This example from [/ui/ceval/package.json](./ceval/package.json) copies stockfish wasms to /public/npm:

```json
    "sync": {
      "node_modules/*stockfish*/*.{js,wasm}": "/public/npm"
    },
```

Sync watch is helpful when you must link a [local version of an npm package](https://github.com/lichess-org/lila/wiki/Lichess-UI-Development#customizing-linked-pnpm-modules). Issues involving chessground, pgn-viewer, or third party dependencies often require this. The sync element ensures that changes to locally linked packages are propagated through the build system to their destinations within the /public folder so they are visible on browser reloads.

## "hash" property

Why hash? Web asset distribution involves frequent caching, and hashes provide a repeatable way to compute URLs as a function of a file's content. ui/build writes the hashes used to cache asset URLs to a manifest.\*.json file. The server uses this manifest to link content-hashed URLs for unique asset versions in every response. Between the client and server, our Content Delivery Network (CDN) caches static assets via edge servers located around the world. Once the first request for a unique URL triggers an initial response from the lichess server, subsequent requests for that same URL from that region do not involve the lichess data center. The CDN edge servers persist cache entries for up to a year, so responses for the same URL are frozen in time without manual intervention.

ui/build automatically hashes compiled and bundled sources such as typescript and scss. For unmanaged assets, like images, fonts, and dynamically loaded npm repo files, you must use "build" / "hash" object entries.

Hash object entries identify files for which a symlink named with their content hash is created within /public/hashed. The symlink points back to the original file (somewhere in /public), ensuring that when the file's content changes on the filesystem, its corresponding symlink gets a new name. Asset URLs use the symlink rather than the base filename, allowing optimal CDN caching and distribution.

"build" / "hash" may contain a single entry or an array of entries. Entries may be bare string globs:

```json
    "hash": [
      "/public/lifat/background/montage*.webp",
      "/public/npm/*",
      "/public/javascripts/**",
      "/public/piece-css/*"
    ]
```
They may also be `{ "glob": "<pattern>", "update": "<package-relative-path>" }` objects. When these are processed, symlinks for globbed files are created in /public/hashed. Then all occurrence of those globbed filenames are replaced with their hashed symlinks within the "update" file's contents. The updated contents are also content-hashed and written to /public/hashed. This is useful when an asset references other files by name and those references must be updated to reflect the hashed URLs. For example: [/ui/common/css/theme/font-face.css](./common/css/theme/font-face.css) is transformed via this hash entry from [/ui/common/package.json](./common/package.json):

```json
    "hash": [
      {
        "glob": "/public/font/*.woff2",
        "update": "css/theme/font-face.css"
      }
    ]
```

Note that "update" files must be package relative.

The nodejs sources for ui/build script are in the [/ui/.build](./.build) folder.
