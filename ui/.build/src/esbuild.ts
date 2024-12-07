import path from 'node:path';
import es from 'esbuild';
import fs from 'node:fs';
import { env, errorMark, colors as c } from './env.ts';
import { type Manifest, updateManifest } from './manifest.ts';
import { readable } from './parse.ts';

const esbuildCtx: es.BuildContext[] = [];
const inlineWatch: fs.FSWatcher[] = [];
let inlineTimer: NodeJS.Timeout;

export async function esbuild(): Promise<void> {
  if (!env.esbuild) return;

  const entryPoints = [];
  for (const pkg of env.building) {
    for (const { module } of pkg.bundle) {
      if (module) entryPoints.push(path.join(pkg.root, module));
    }
  }
  entryPoints.sort();
  const ctx = await es.context({
    entryPoints,
    bundle: true,
    metafile: true,
    treeShaking: true,
    splitting: true,
    format: 'esm',
    target: 'es2020',
    logLevel: 'silent',
    sourcemap: !env.prod,
    minify: env.prod,
    outdir: env.jsOutDir,
    entryNames: '[name].[hash]',
    chunkNames: 'common.[hash]',
    plugins,
  });
  if (env.watch) {
    ctx.watch();
    esbuildCtx.push(ctx);
  } else {
    await ctx.rebuild();
    await ctx.dispose();
  }
}

export async function stopEsbuildWatch(): Promise<void> {
  const proof = Promise.allSettled(esbuildCtx.map(x => x.dispose()));
  for (const w of inlineWatch) w.close();
  inlineWatch.length = 0;
  esbuildCtx.length = 0;
  await proof;
}

const plugins = [
  {
    name: 'onBundleDone',
    setup(build: es.PluginBuild) {
      build.onEnd(async (result: es.BuildResult) => {
        esbuildLog(result.errors, true);
        esbuildLog(result.warnings);
        env.done(result.errors.length, 'esbuild');
        if (result.errors.length === 0) jsManifest(result.metafile!);
      });
    },
  },
];

function esbuildLog(msgs: es.Message[], error = false): void {
  for (const msg of msgs) {
    const file = msg.location?.file.replace(/^[./]*/, '') ?? '<unknown>';
    const line = msg.location?.line
      ? `:${msg.location.line}`
      : '' + (msg.location?.column ? `:${msg.location.column}` : '');
    const srcText = msg.location?.lineText;
    env.log(`${error ? errorMark : c.warn('WARNING')} - '${c.cyan(file + line)}' - ${msg.text}`, {
      ctx: 'esbuild',
    });
    if (srcText) env.log('  ' + c.magenta(srcText), { ctx: 'esbuild' });
  }
}

async function jsManifest(meta: es.Metafile = { inputs: {}, outputs: {} }) {
  for (const w of inlineWatch) w.close();
  inlineWatch.length = 0;
  clearTimeout(inlineTimer);

  const newJsManifest: Manifest = {};
  for (const [filename, info] of Object.entries(meta.outputs)) {
    const out = parsePath(filename);
    if (!out) continue;
    if (out.name === 'common') {
      out.name = `common.${out.hash}`;
      newJsManifest[out.name] = {};
    } else newJsManifest[out.name] = { hash: out.hash };
    const imports: string[] = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = parsePath(imp.path);
        if (path) imports.push(`${path.name}.${path.hash}.js`);
      }
    }
    newJsManifest[out.name].imports = imports;
  }
  await inlineManifest(newJsManifest);
}

async function inlineManifest(js: Manifest) {
  const makeWatchers = env.watch && inlineWatch.length === 0;
  let success = true;
  for (const pkg of env.building) {
    for (const bundle of pkg.bundle ?? []) {
      if (!bundle.inline) continue;

      const inlineSrc = path.join(pkg.root, bundle.inline);
      const moduleName = bundle.module
        ? path.basename(bundle.module, '.ts')
        : path.basename(bundle.inline, '.inline.ts');
      const packageError = `${errorMark} - Package error ${c.blue(JSON.stringify(bundle))}`;

      if (!(await readable(inlineSrc))) {
        env.log(packageError);
        for (const w of inlineWatch) w.close();
        inlineWatch.length = 0;
        if (!env.watch) env.exit('Failed'); // all inline sources must exist
      }

      try {
        const res = await es.transform(await fs.promises.readFile(inlineSrc), {
          minify: true,
          loader: 'ts',
        });
        esbuildLog(res.warnings);
        js[moduleName] ??= {};
        js[moduleName].inline = res.code;
      } catch (e) {
        if (e && typeof e === 'object' && 'errors' in e) esbuildLog((e as es.TransformFailure).errors, true);
        else env.log(`${packageError} - ${JSON.stringify(e)}`);
        if (env.watch) success = false;
        else env.exit('Failed');
      }
      if (makeWatchers)
        inlineWatch.push(
          fs.watch(inlineSrc, () => {
            clearTimeout(inlineTimer);
            inlineTimer = setTimeout(() => inlineManifest(js), 200);
          }),
        );
    }
  }
  if (success) updateManifest({ js });
}

function parsePath(path: string) {
  const match = path.match(/\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/);
  return match ? { name: match[1], hash: match[2] } : undefined;
}
