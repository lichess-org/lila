import * as fs from 'node:fs';
import * as path from 'node:path';
import * as ps from 'node:process';
import { buildModules, build, stop } from './build';
import { env } from './main';
import { globArray } from './parse';
import { stopTsc, tsc } from './tsc';

const watchers: fs.FSWatcher[] = [];

let reinitTimeout: NodeJS.Timeout | undefined;
let tscTimeout: NodeJS.Timeout | undefined;

export function stopTickling() {
  for (const w of watchers) w.close();
  watchers.length = 0;
  clearTimeout(tscTimeout);
  clearTimeout(reinitTimeout);
  reinitTimeout = undefined;
}

export async function startTickling(mods: string[]) {
  if (!env.watch) return;
  const typePkgs = await globArray('*/package.json', { cwd: env.typesDir, abs: true });
  const typings = await globArray('*/*.d.ts', { cwd: env.typesDir, abs: true });

  const tscChange = (t: any) => {
    if (reinitTimeout) return;
    stopTsc();
    clearTimeout(tscTimeout);
    tscTimeout = setTimeout(() => reinitTimeout ?? tsc(), 2000);
  };
  const packageChange = async () => {
    if (env.rebuild) {
      clearTimeout(tscTimeout);
      clearTimeout(reinitTimeout);
      await stop();
      reinitTimeout = setTimeout(() => build(mods), 2000);
      return;
    }
    env.warn('Exiting due to package.json change');
    env.warn('Use -r/--rebuild to watch, reinstall, and rebuild rather than exit');
    ps.exit(0);
  };

  watchers.push(fs.watch(path.join(env.rootDir, 'package.json'), packageChange));
  for (const p of typePkgs) watchers.push(fs.watch(p, packageChange));
  for (const t of typings) watchers.push(fs.watch(t, tscChange));
  for (const mod of buildModules) {
    watchers.push(fs.watch(path.join(mod.root, 'package.json'), packageChange));
    watchers.push(fs.watch(path.join(mod.root, 'tsconfig.json'), tscChange));
  }
}
