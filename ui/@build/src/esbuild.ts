import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as es from 'esbuild';
import { preModule, buildModules } from './build';
import { env, errorMark, colors as c } from './main';

export async function esbuild(): Promise<void> {
  if (!env.esbuild) return;
  const entryPoints: { [key: string]: string } = {};
  for (const mod of buildModules) {
    preModule(mod);
    if (mod.bundle) {
      for (const r of mod.bundle) {
        entryPoints[r.output] = path.join(mod.root, r.input);
      }
    }
  }
  const opts: es.BuildOptions = {
    sourcemap: !env.prod,
    define: {
      __info__: JSON.stringify({
        date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
        commit: cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim(),
        message: cps.execSync('git log -1 --pretty=%s', { encoding: 'utf-8' }).trim(),
      }),
    },
    format: 'iife',
    target: 'es2018',
    logLevel: 'silent',
    bundle: true,
    outdir: env.jsDir,
    minify: env.prod,
    entryPoints: entryPoints,
    treeShaking: true,
    outExtension: { '.js': env.prod ? '.min.js' : '.js' },
    plugins: [onEndPlugin],
  };
  try {
    env.watch ? await (await es.context(opts)).watch() : await es.build(opts);
  } catch (e: any) {
    env.log(e, { error: true });
  }
}

const onEndPlugin = {
  name: 'lichessOnEnd',
  setup(build: es.PluginBuild) {
    build.onEnd((result: es.BuildResult) => {
      for (const err of result.errors) {
        esbuildMessage(err, true);
      }
      for (const warn of result.warnings) {
        esbuildMessage(warn);
      }
      env.done(result.errors.length, 'esbuild');
    });
  },
};

function esbuildMessage(msg: es.Message, error = false) {
  const file = msg.location?.file.replace(/^[./]*/, '') ?? '<unknown>';
  const line = msg.location?.line ? `line ${msg.location.line} of ` : '';
  const srcText = msg.location?.lineText;
  env.log(`${error ? errorMark : c.warn('WARNING')} - ${line}'${c.cyan(file)}': ${msg.text}`, { ctx: 'esbuild' });
  if (srcText) env.log('  ' + c.magenta(srcText), { ctx: 'esbuild' });
}
