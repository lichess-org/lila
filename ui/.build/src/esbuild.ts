import * as path from 'node:path';
import * as es from 'esbuild';
import { preModule } from './build';
import { env, errorMark, colors as c } from './main';
import { js as jsManifest } from './manifest';

const bundles = new Map<string, string>();
const esbuildCtx: es.BuildContext[] = [];

export async function stopEsbuild() {
  const proof = Promise.allSettled(esbuildCtx.map(x => x.dispose()));
  esbuildCtx.length = 0;
  bundles.clear();
  return proof;
}

export async function esbuild(): Promise<void> {
  if (!env.esbuild) return;

  const entryPoints = [];
  for (const mod of env.building) {
    preModule(mod);
    for (const bundle of mod.bundles ?? []) {
      entryPoints.push(path.join(mod.root, bundle));
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
    target: 'es2018',
    logLevel: 'silent',
    sourcemap: !env.prod,
    minify: env.prod,
    outdir: env.jsDir,
    entryNames: '[name].[hash]',
    chunkNames: 'common.[hash]',
    plugins: [onEndPlugin],
  });
  if (env.watch) {
    ctx.watch();
    esbuildCtx.push(ctx);
  } else {
    await ctx.rebuild();
    await ctx.dispose();
  }
}

const onEndPlugin = {
  name: 'lichessOnEnd',
  setup(build: es.PluginBuild) {
    build.onEnd((result: es.BuildResult) => {
      for (const err of result.errors) esbuildMessage(err, true);
      for (const warn of result.warnings) esbuildMessage(warn);
      env.done(result.errors.length, 'esbuild');
      if (result.errors.length) return;
      jsManifest(result.metafile!);
    });
  },
};

function esbuildMessage(msg: es.Message, error = false) {
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
