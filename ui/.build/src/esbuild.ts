import es from 'esbuild';
import fs from 'node:fs';
import { join, basename } from 'node:path';
import { env, errorMark, warnMark, c } from './env.ts';
import { type Manifest, updateManifest } from './manifest.ts';
import { makeTask, stopTask } from './task.ts';
import { definedMap } from './algo.ts';

let esbuildCtx: es.BuildContext | undefined;

export async function esbuild(): Promise<any> {
  if (!env.begin('esbuild')) return;

  const options: es.BuildOptions = {
    bundle: true,
    metafile: true,
    treeShaking: true,
    splitting: true,
    format: 'esm',
    target: 'es2018',
    supported: { bigint: true },
    logLevel: 'silent',
    sourcemap: !env.prod,
    minify: env.prod,
    outdir: env.jsOutDir,
    entryNames: '[name].[hash]',
    chunkNames: 'lib.[hash]',
    plugins,
  };

  await fs.promises.mkdir(env.jsOutDir).catch(() => {});
  return Promise.all([
    inlineTask(),
    makeTask({
      key: 'bundle',
      ctx: 'esbuild',
      debounce: 300,
      noEnvStatus: true,
      globListOnly: true,
      includes: env.building.flatMap(pkg =>
        definedMap(
          pkg.bundle.map(bundle => bundle.module),
          path => ({ cwd: pkg.root, path }),
        ),
      ),
      execute: async entryPoints => {
        await esbuildCtx?.dispose();
        entryPoints.sort();
        esbuildCtx = await es.context({ ...options, entryPoints });
        if (env.watch) esbuildCtx.watch();
        else {
          await esbuildCtx.rebuild();
          await esbuildCtx.dispose();
        }
      },
    }),
  ]);
}

export async function stopEsbuild(): Promise<void> {
  stopTask(['bundle', 'inline']);
  await esbuildCtx?.dispose();
  esbuildCtx = undefined;
}

function inlineTask() {
  const js: Manifest = {};
  const inlineToModule: Record<string, string> = {};
  for (const [pkg, bundle] of env.tasks('bundle'))
    if (bundle.inline)
      inlineToModule[join(pkg.root, bundle.inline)] = bundle.module
        ? basename(bundle.module, '.ts')
        : basename(bundle.inline, '.inline.ts');
  return makeTask({
    key: 'inline',
    debounce: 300,
    includes: env.building.flatMap(pkg =>
      definedMap(
        pkg.bundle.map(b => b.inline),
        path => ({ cwd: pkg.root, path }),
      ),
    ),
    execute: (_, inlines) =>
      Promise.all(
        inlines.map(async inlineSrc => {
          const moduleName = inlineToModule[inlineSrc];
          try {
            const res = await es.transform(await fs.promises.readFile(inlineSrc), {
              minify: true,
              loader: 'ts',
              target: 'es2018',
            });
            esbuildLog(res.warnings);
            js[moduleName] ??= {};
            js[moduleName].inline = res.code;
          } catch (e) {
            if (e && typeof e === 'object' && 'errors' in e)
              esbuildLog((e as es.TransformFailure).errors, true);
            throw '';
          }
        }),
      ).then(() => updateManifest({ js })),
  });
}

function bundleManifest(meta: es.Metafile = { inputs: {}, outputs: {} }) {
  const js: Manifest = {};
  for (const [filename, info] of Object.entries(meta.outputs)) {
    const out = splitPath(filename);
    if (!out) continue;
    if (out.name === 'lib') {
      out.name = `lib.${out.hash}`;
      js[out.name] = {};
    } else js[out.name] = { hash: out.hash };
    const imports: string[] = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = splitPath(imp.path);
        if (path) imports.push(`${path.name}.${path.hash}.js`);
      }
    }
    js[out.name].imports = imports;
  }
  updateManifest({ js });
}

function esbuildLog(msgs: es.Message[], error = false): void {
  for (const msg of msgs) {
    const file = msg.location?.file.replace(/^[./]*/, '') ?? '<unknown>';
    const line = msg.location?.line
      ? `:${msg.location.line}`
      : '' + (msg.location?.column ? `:${msg.location.column}` : '');
    const srcText = msg.location?.lineText;
    env.log(`${error ? errorMark : warnMark} - '${c.cyan(file + line)}' - ${msg.text}`, 'esbuild');
    if (srcText) env.log('  ' + c.magenta(srcText), 'esbuild');
  }
}

function splitPath(path: string) {
  const match = path.match(/\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/);
  return match ? { name: match[1], hash: match[2] } : undefined;
}

// our html minifier will only process characters between the first two backticks encountered
// so:
//   $html`     <div>    ${    x ?      `<- 2nd backtick   ${y}${z}` : ''    }     </div>`
//
// minifies (partially) to:
//   `<div> ${ x ? `<- 2nd backtick   ${y}${z}` : ''    }     </div>`
//
// nested template literals in interpolations are unchanged and still work, but they
// won't be minified.

const plugins = [
  {
    name: '$html',
    setup(build: es.PluginBuild) {
      build.onLoad({ filter: /\.ts$/ }, async (args: es.OnLoadArgs) => ({
        loader: 'ts',
        contents: (await fs.promises.readFile(args.path, 'utf8')).replace(
          /\$html`([^`]*)`/g,
          (_, s) => `\`${s.trim().replace(/\s+/g, ' ')}\``,
        ),
      }));
    },
  },
  {
    name: 'onBundleDone',
    setup(build: es.PluginBuild) {
      build.onEnd(async (result: es.BuildResult) => {
        esbuildLog(result.errors, true);
        esbuildLog(result.warnings);
        env.begin('esbuild');
        env.done('esbuild', result.errors.length > 0 ? -3 : 0);
        if (result.errors.length === 0) bundleManifest(result.metafile!);
      });
    },
  },
];
