import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as es from 'esbuild';
import { preModule, bundleDone } from './build';
import { LichessModule, env, errorMark, colors as c } from './main';

export async function esbuildWatch(todo: LichessModule[]): Promise<void> {
  const entryPoints: { [key: string]: string } = {};
  for (const mod of todo) {
    if (mod.bundle)
      for (const r of mod.bundle) {
        entryPoints[r.output] = path.join(mod.root, r.input);
        // if/when globalName option becomes an array or a field in entryPoints, use r.importName
        // and we can remove the (window as any).LichessModule = stuff from the module typescript
        if (r.isMain) preModule(mod);
      }
  }
  try {
    await es.build({
      sourcemap: 'inline',
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
      watch: true,
      entryPoints: entryPoints,
      plugins: [/*onStartPlugin, onLoadPlugin,*/ onEndPlugin],
    });
  } catch (e: any) {
    env.log(e, { error: true });
  }
}
/*
const onStartPlugin = {
  name: 'bleepOnStart',
  setup(build: es.PluginBuild) {
    build.onStart(() => env.log(c.grey('Bundling') + ' modules', { ctx: 'esbuild' }));
  },
};

const fileFilter = new RegExp(`\\${path.sep}ui\\${path.sep}(.+\\.ts)$`);
const onLoadPlugin = {
  // more like onMurderScrollbackBuffer
  name: 'bleepOnLoad',
  setup(build: es.PluginBuild) {
    build.onLoad({ filter: fileFilter }, (o: es.OnLoadArgs): es.OnLoadResult | undefined => {
      env.log(`Bundling '${c.cyan(fileFilter.exec(o.path)![1])}'`, { ctx: 'esbuild' });
      return undefined;
    });
  },
};*/

const onEndPlugin = {
  name: 'bleepOnEnd',
  setup(build: es.PluginBuild) {
    build.onEnd((result: es.BuildResult) => {
      for (const err of result.errors) {
        esbuildMessage(err, true);
      }
      for (const warn of result.warnings) {
        esbuildMessage(warn);
      }
      const report = result.errors.length === 0 ? `${c.good('No errors')} - ` : `${result.errors} errors - `;
      env.log(`${report}${c.grey('Watching')}...`, { ctx: 'esbuild' });

      bundleDone(0);
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
