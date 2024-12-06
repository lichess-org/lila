import fs from 'node:fs';
import cps from 'node:child_process';
import ps from 'node:process';
import path from 'node:path';
import { parsePackages, globArray } from './parse.ts';
import { tsc, stopTscWatch } from './tsc.ts';
import { sass, stopSass } from './sass.ts';
import { esbuild, stopEsbuildWatch } from './esbuild.ts';
import { sync, stopSync } from './sync.ts';
import { stopManifest } from './manifest.ts';
import { env, errorMark, colors as c } from './env.ts';
import { i18n, stopI18nWatch } from './i18n.ts';
import { unique } from './algo.ts';
import { clean } from './clean.ts';

export async function build(pkgs: string[]): Promise<void> {
  if (env.install) cps.execSync('pnpm install', { cwd: env.rootDir, stdio: 'inherit' });
  if (!pkgs.length) env.log(`Parsing packages in '${c.cyan(env.uiDir)}'`);

  ps.chdir(env.uiDir);
  await parsePackages();

  pkgs
    .filter(x => !env.packages.has(x))
    .forEach(x => env.exit(`${errorMark} - unknown package '${c.magenta(x)}'`));

  env.building =
    pkgs.length === 0 ? [...env.packages.values()] : unique(pkgs.flatMap(p => env.transitiveDeps(p)));

  if (pkgs.length) env.log(`Building ${c.grey(env.building.map(x => x.name).join(', '))}`);

  await Promise.allSettled([
    fs.promises.mkdir(env.jsOutDir),
    fs.promises.mkdir(env.cssOutDir),
    fs.promises.mkdir(env.hashOutDir),
    fs.promises.mkdir(env.themeGenDir),
    fs.promises.mkdir(env.buildTempDir),
  ]);

  await Promise.all([sass(), sync(), i18n()]);
  await Promise.all([tsc(), esbuild()]);
  if (env.watch) monitor(pkgs);
}

export async function stopBuildWatch(): Promise<void> {
  for (const w of watchers) w.close();
  watchers.length = 0;
  clearTimeout(tscTimeout);
  clearTimeout(packageTimeout);
  tscTimeout = packageTimeout = undefined;
  stopSass();
  stopSync();
  stopI18nWatch();
  stopManifest();
  await Promise.allSettled([stopTscWatch(), stopEsbuildWatch()]);
}

const watchers: fs.FSWatcher[] = [];

let packageTimeout: NodeJS.Timeout | undefined;
let tscTimeout: NodeJS.Timeout | undefined;

async function monitor(pkgs: string[]): Promise<void> {
  const [typePkgs, typings] = await Promise.all([
    globArray('*/package.json', { cwd: env.typesDir }),
    globArray('*/*.d.ts', { cwd: env.typesDir }),
  ]);
  const tscChange = async () => {
    if (packageTimeout) return;
    stopManifest();
    await Promise.allSettled([stopTscWatch(), stopEsbuildWatch()]);
    clearTimeout(tscTimeout);
    tscTimeout = setTimeout(() => {
      if (packageTimeout) return;
      tsc().then(esbuild);
    }, 2000);
  };
  const packageChange = async () => {
    if (env.watch && env.install) {
      clearTimeout(tscTimeout);
      clearTimeout(packageTimeout);
      await stopBuildWatch();
      packageTimeout = setTimeout(() => clean().then(() => build(pkgs)), 2000);
      return;
    }
    env.warn('Exiting due to package.json change');
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
