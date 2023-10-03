import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as es from 'esbuild';
import { preModule, buildModules } from './build';
import { env, errorMark, colors as c } from './main';

const typeBundles = new Map<string, Map<string, string>>();

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

  for (const mod of buildModules) {
    preModule(mod);
    for (const tpe in mod.bundles) {
      if (!typeBundles.has(tpe)) typeBundles.set(tpe, new Map());
      for (const r of mod.bundles[tpe]) typeBundles.get(tpe)?.set(r.output, path.join(mod.root, r.input));
    }
  }
  for (const [tpe, bundles] of typeBundles) {
    const ctx = await es.context({
      sourcemap: !env.prod,
      define,
      format: tpe as es.Format,
      target: 'es2018',
      logLevel: 'silent',
      splitting: env.split && tpe === 'esm',
      bundle: true,
      outdir: env.jsDir,
      minify: env.prod,
      entryPoints: Object.fromEntries(bundles),
      treeShaking: true,
      outExtension: { '.js': env.prod ? '.min.js' : '.js' },
      plugins: [onEndPlugin],
    });
    if (env.watch) ctx.watch();
    else {
      await ctx.rebuild();
      ctx.dispose();
    }
  }
}

const onEndPlugin = {
  name: 'lichessOnEnd',
  setup(build: es.PluginBuild) {
    build.onEnd((result: es.BuildResult) => {
      for (const err of result.errors) esbuildMessage(err, true);
      for (const warn of result.warnings) esbuildMessage(warn);
      typeBundles.delete(typeBundles.keys().next().value);
      if (result.errors.length || !typeBundles.size) env.done(result.errors.length, 'esbuild');
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
