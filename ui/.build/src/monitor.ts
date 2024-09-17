import fs from 'node:fs';
import path from 'node:path';
import ps from 'node:process';
import { build, stop } from './build';
import { env } from './main';
import { globArray } from './parse';
import { stopTsc, tsc } from './tsc';
import { stopEsbuild, esbuild } from './esbuild';

const watchers: fs.FSWatcher[] = [];

let reinitTimeout: NodeJS.Timeout | undefined;
let tscTimeout: NodeJS.Timeout | undefined;

export function stopMonitor(): void {
  for (const w of watchers) w.close();
  watchers.length = 0;
  clearTimeout(tscTimeout);
  clearTimeout(reinitTimeout);
  reinitTimeout = undefined;
}

export async function startMonitor(pkgs: string[]): Promise<void> {
  if (!env.watch) return;
  const typePkgs = await globArray('*/package.json', { cwd: env.typesDir });
  const typings = await globArray('*/*.d.ts', { cwd: env.typesDir });
  const tscChange = async() => {
    if (reinitTimeout) return;
    await stopTsc();
    await stopEsbuild();
    clearTimeout(tscTimeout);
    tscTimeout = setTimeout(() => {
      if (reinitTimeout) return;
      esbuild(tsc());
    }, 2000);
  };
  const packageChange = async() => {
    if (env.rebuild) {
      clearTimeout(tscTimeout);
      clearTimeout(reinitTimeout);
      await stop();
      reinitTimeout = setTimeout(() => build(pkgs), 2000);
      return;
    }
    env.warn('Exiting due to package.json change');
    env.warn('Use --rebuild / -r to rebuild rather than exit');
    ps.exit(0);
  };

  watchers.push(fs.watch(path.join(env.rootDir, 'package.json'), packageChange));
  for (const p of typePkgs) watchers.push(fs.watch(p, packageChange));
  for (const t of typings) watchers.push(fs.watch(t, tscChange));
  for (const pkg of env.building) {
    watchers.push(fs.watch(path.join(pkg.root, 'package.json'), packageChange));
    watchers.push(fs.watch(path.join(pkg.root, 'tsconfig.json'), tscChange));
  }
}
