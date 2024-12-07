# Getting Started

Client assets are built with the [/ui/build](./build) script.

```bash
ui/build --help
```

You can usually start up `ui/build -wc` and leave it running. This performs a clean build and continuously rebuilds source files when changes are detected. Keep an eye on stdout for build errors. On success, reload the browser page for the results.

# UI packages

A package.json file describes the hierarchy of source files, dependencies, and build steps for each folder in the /ui pnpm monorepo.

We must tell tsc how to resolve static workspace imports from external sources back to declaration (\*.d.ts) files in the imported package's dist folder. When a single declaration barrel describes all package types, we use the "types" property as shown in this example from [/ui/voice/package.json](./voice/package.json):

```json
  "types": "dist/voice.d.ts",
```

When more granular access to a package is needed, we use the "typesVersion" property to expose selective imports within the dist folder to tsc along with an optional "typings" property to identify any main barrel within that "typesVersion" mapping.

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

The [esbuild bundler](https://esbuild.github.io/getting-started/#your-first-bundle) works in tandem with tsc on typescript sources but resolves imports differently. It reads an "exports" object to resolve static workspace imports from other packages directly to the imported package's typescript source files.

For example, imports of common package code from a dependent package like this example from [/ui/opening/src/opening.ts](./opening/src/opening.ts):

```typescript
import { initMiniBoards } from 'common/miniBoard';
import { requestIdleCallback } from 'common';
```

Are mapped back to common/src by this snippet from [/ui/common/package.json](./common/package.json):

```json
  "exports": {
    ".": {
      ".": "./src/common.ts",
      "./*": "./src/*.ts"
    }
  },
```

While esbuild may bundle imported code directly into the entry point module, it may also split imported code into "common" chunk modules that are shared and imported by other workspace modules. This chunked approach is called code splitting and reduces the overall footprint of asset transfers over the wire and within the browser cache.

Because the client is a monorepo consisting of many packages, it is necessary to express dependencies on another workspace package with a "dependencies" object property keyed by the package name with "workspace:\*" as the value. That tells [pnpm](https://pnpm.io) and ui/build that the dependency is a workspace package folder located directly within /ui.

```json
  "dependencies": {
    "common": "workspace:*",
    "some-npm-package": "^1.0.0",
    "local-override-of-an-npm-package": "link:../local-override-of-an-npm-package"
  },
```

We do not classify devDependencies because these workspace packages are not built and published to npm for others to use without building. There is no useful distinction between dependencies and devDependencies when we're always building static assets directly for lila.

## Build & Bundle

We define a custom, top-level "build" object to describe how [/ui/build](./build) generates assets for the website.

```json
  "build": {
```

Understanding the mechanisms expressed by properties of the "build" object will help you create / edit packages and define how dependencies are managed.

The "bundle" property tells esbuild which javascript modules should be created as named entry points. Most correspond to a server controller and scala module found in [/app](../app) and [/modules](../modules) respectively. These usually generate the html DOM on which the javascript operates.

Path elements inside the "bundle" property are resolved relative to the folder where the package.json resides. A path may specify a glob pattern to match multiple module sources and bundle each into named javascript entry points.

This example from [/ui/analyse/package.json](./analyse/package.json) matches analyse.ts, analyse.nvui.ts, analyse.study.ts, analyse.study.topic.form.ts, and analyse.study.tour.ts from various places in the folder hierarchy within analyse/src:

```json
    "bundle": "src/**/analyse.*ts",
```

All bundled output is transpiled to esm modules in the /public/compiled folder. Filenames are composed with the module source basename, followed by an 8 char content hash, and ending with .js.

Multiple bundles specs may be given in an array, elements of which may be globs like above or bare paths as shown in this [/ui/site/package.json](./site/package.json) example:

```json
    "bundle": [
      "src/site.lpvEmbed.ts",
      "src/site.puzzleEmbed.ts",
      "src/site.tvEmbed.ts",
```

A bundle may also be described with an object containing a "module" path and an "inline" path. The "module" value serves the same purpose as in the direct path examples above - naming the source module for an entry point. But the "inline" value identifies a secondary ts source from which ui/build will emit javascript statements into a manifest.\*.json entry for the parent module.

When that parent module is requested by a browser, the lila server injects its inline statements into a \<script> tag following the assembled DOM within the \<body> element. This allows blocking setup code to manipulate the DOM based on viewport calculations before rendering. This should be done sparingly and globs are not supported here. The final object in [/ui/site/package.json](./site/package.json)'s bundle array shows a valid example:

```json
      {
        "module": "src/site.ts",
        "inline": "src/site.inline.ts"
      }
```

## Sync

The sync object describes filesystem copies performed by ui/build from the package folder to elsewhere in the lila repo. Sync property keys identify source assets and are resolved relative to the package folder. Sync property values locate the destination folder relative to repo root. You'll typically want to copy from the package folder to somewhere in /public which is why key and value paths are resolved with different roots. In watch mode, ui/build will copy assets to the destination folder whenever they change.

A typical usage for sync is to copy elements from node_modules (an npm package dependency) to the /public/npm folder when an external package needs to be loaded dynamically, usually because it is too large to bundle. This example from [/ui/ceval/package.json](./ceval/package.json) copies stockfish wasms to /public/npm:

```json
    "sync": {
      "node_modules/*stockfish*/*.{js,wasm}": "public/npm"
    },
```

Sync is helpful when you must link a local version of an npm package using [pnpm link](https://github.com/lichess-org/lila/wiki/Lichess-UI-Development#customizing-linked-pnpm-modules) or a "link:..." version specifier in package.json. Isolating issues involving chessground, pgn-viewer, or third party dependencies will often require this. During `ui/build -w` watch, the sync element ensures that changes to locally linked packages are propagated through the build system to their destinations within the /public folder so they are visible on browser reloads.

## Hash

Web asset distribution involves the caching of URLs, and hashes provide a repeatable way to compute URLs as a function of a file's content. Our Content Delivery Network (CDN) takes advantage of URL/key uniqueness to store and deliver static assets via caching edge servers located around the world. Once the first request for a new URL triggers an initial response from the lichess server, subsequent delivery of that asset version to IPs in that region do not involve lichess. The CDN persists cache entries for up to a year, so responses from static URLs are effectively frozen in place barring manual intervention.

ui/build calculates and writes all hashes used to determine asset URLs to a manifest.\*.json file. This file is used by the lila server to tell browsers what they need. Javascript and css assets built from lichess sources are hashed automatically, but the "build" / "hash" section within package.json describes assets that must be hashed separately. These include images, fonts, and packages of js & css from the npmjs repository that we don't compile but must be exposed through our content distribution strategy.

Because these unmanaged assets originate in or are copied to the /public folder during the build process, all paths within the "hash" property resolve relative to /public.

ui/build computes a sha256 checksum of each matched asset's content, using a portion of that checksum to make a hash, then creates a symlink with that hash in the name pointing back to the original file. All links are created within /public/hashed. When a source file's content changes on the filesystem, its corresponding symlink will get a new name. This changes the object's URL and forces our CDN to create a fresh cache entry that will propagate through their edge server caches in distribution. Once again, lila is kept informed by ui/build through entries it writes to manifest.\*.json.

```json
    "hash": [
      "font/lichess.woff",
      "font/lichess.woff2",
      "lifat/background/montage*.webp",
      "npm/*",
      "javascripts/**",
      "piece-css/*"
    ]
```

Above we see the hash property in [/ui/site/package.json](./site/package.json) where we match the listed web fonts as well as files in lifat/background that start with montage and end with .webp. The npm/\* glob requests hashes for all top level files in public/npm, and at this point it's helpful to note that matched assets are hashed during manifest creation AFTER "sync" operations have completed for all packages.

In the ceval/package.json example we saw where stockfish wasms are synced to /public/npm. Here in site/package.json, those synced stockfish wasms are subsequently matched by globs at their copy destinations and symlinked in /public/hashed for efficient distribution. [/ui/site/package.json](./site/package.json) is where we hash unmanaged assets that don't really fit anywhere else.

The double asterisk in the javascripts/\*\* glob match everything inside its folder hierarchy.

And that's about it for package.json. The nodejs sources for ui/build script are in the [/ui/.build](./.build) folder. Have a glance if something goes wrong or you have questions beyond the scope of this readme.

# Testing

The frontend uses the [Vitest](https://vitest.dev/) testing framework. Trevlar likes tests. Please write tests.

```bash
pnpm test
## or
pnpm test:watch
```
