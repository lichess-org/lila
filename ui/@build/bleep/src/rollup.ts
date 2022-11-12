import * as rup from 'rollup';
import * as fs from 'node:fs';
import * as path from 'node:path';
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import { preModule, bundleDone, resetTimer } from './build';
import { LichessModule, LichessBundle, env, colors as c } from './main';

export function rollupWatch(todo: LichessModule[]): void {
  const outputToHostMod = new Map<string, LichessModule>();
  const triggerScriptCmds = new Set<string>();
  const rollups: rup.RollupOptions[] = [];

  todo.forEach(mod => {
    mod.bundle?.forEach(r => {
      const options = rollupOptions(r);
      const output = path.join(env.jsDir, `${r.output}.js`);
      outputToHostMod.set(output, mod);
      if (r.isMain) triggerScriptCmds.add(output);
      rollups.push(options);
    });
  });
  if (!rollups.length) {
    bundleDone(0);
    return;
  }
  let moduleName = 'unknown';
  let count = 0;

  rup.watch(rollups).on('event', (e: rup.RollupWatcherEvent) => {
    if (e.code === 'START') {
      resetTimer(true);
      resetTimer();
    } else if (e.code === 'END') {
      bundleDone(count);
      count = 0;
    } else if (e.code === 'ERROR') {
      rollupError(e.error, moduleName);
    } else if (e.code === 'BUNDLE_START') {
      resetTimer();
      const output = e.output.length > 0 ? e.output[0] : '';
      const hostMod = outputToHostMod.get(output);
      moduleName = hostMod?.name || 'unknown';
      if (triggerScriptCmds.has(output)) preModule(hostMod);
    } else if (e.code === 'BUNDLE_END') {
      const output = e.output.length > 0 ? e.output[0] : '';
      const result = fs.existsSync(output)
        ? `bundled '${c.cyan(path.basename(output))}' - `
        : `not found '${c.red(output)}' - `;
      env.log(result + c.grey(`${e.duration}ms`), { ctx: 'rollup' });
      e.result?.close();
      count++;
    }
  });
}

function rollupError(err: rup.RollupError, name: string) {
  if (!err.code) {
    env.log(err, { ctx: name, error: true });
    return;
  }
  const filename = err.loc?.file || err.id || name;
  const loc = err.loc ? `line ${err.loc.line} column ${err.loc.column} of ` : '';
  const preamble = c.red(err.code) + ` in ${loc}'${c.cyan(filename)}'`;
  env.log(`${preamble}\n${err.frame ? c.red(err.frame) : ''}`, { ctx: c.red(name) });
  env.log(c.red(`*** ${name} module bundle failed! ***`), { ctx: c.red(name) });
}

function rollupOptions(o: LichessBundle): rup.RollupWatchOptions {
  const modDir = o.hostMod.root;
  const plugins = (o.plugins || []).concat(
    o.hostMod.hasTsconfig
      ? [typescript({ tsconfig: path.join(modDir, 'tsconfig.json') }), resolve(), commonjs({ extensions: ['.js'] })]
      : []
  );
  return {
    input: path.join(modDir, o.input),
    plugins: plugins,
    onwarn: o.onWarn,
    output: {
      format: 'iife',
      name: o.importName,
      file: path.join(env.jsDir, `${o.output}.js`),
      generatedCode: { preset: 'es2015', constBindings: false },
    },
  };
}
