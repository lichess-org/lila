import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as fs from 'node:fs';
import * as es from 'esbuild';
import { preModule, buildModules } from './build';
import { env, errorMark, colors as c } from './main';
import { globArray } from './parse';

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

  const define: { [_: string]: string } = {
    __info__: JSON.stringify({
      date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
      commit: cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim(),
      message: cps.execSync('git log -1 --pretty=%s', { encoding: 'utf-8' }).trim(),
    }),
    __debug__: String(env.debug),
  };

  const entryPoints = await globArray(`${env.cssTempDir}/*.css`);
  for (const mod of buildModules) {
    preModule(mod);
    for (const r of mod.bundles ?? []) {
      entryPoints.push(path.join(mod.root, r.input));
    }
  }

  const ctx = await es.context({
    define,
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
    outdir: env.outDir,
    loader: { '.css': 'file' },
    entryNames: 'compiled/[name].[hash]',
    assetNames: 'css/[name].[hash]',
    chunkNames: 'compiled/[name].[hash]',
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
      buildManifest(result.metafile!);
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

function parsePath(path: string) {
  const matchers = {
    css: /\.\.\/\.\.\/public\/css\/(.*)\.([A-Z0-9]+)\.css$/,
    js: /\.\.\/\.\.\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/,
  };
  for (const [type, re] of Object.entries(matchers)) {
    const match = path.match(re);
    if (match) return { type, name: match[1], hash: match[2] };
  }
  return undefined;
}

async function buildManifest(meta: es.Metafile) {
  const manifest: any = {};
  for (const [file, info] of Object.entries(meta.outputs)) {
    const out = parsePath(file);
    if (!out) continue;
    let { type, name, hash } = out;
    if (!manifest[type]) manifest[type] = {};
    if (name === 'chunk') {
      name = `chunk-${hash}`;
      manifest[type][name] = {};
    } else if (!manifest[type][name]) manifest[type][name] = { hash };
    if (type === 'css') continue;
    manifest[type][name].imports = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = parsePath(imp.path);
        if (path) manifest[type][name].imports.push(`${path.name}.${path.hash}.${path.type}`);
      }
    }
  }
  await fs.promises.writeFile(
    path.join(env.confDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`),
    JSON.stringify(manifest, null, env.prod ? undefined : 2),
  );
  return manifest;
}
