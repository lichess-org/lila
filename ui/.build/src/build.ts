import fs from 'node:fs';
import { relative, join } from 'node:path';
import { execSync } from 'node:child_process';
import { chdir } from 'node:process';
import { parsePackages } from './parse.ts';
import { makeTask, stopTask } from './task.ts';
import { tsc, stopTsc } from './tsc.ts';
import { sass, stopSass } from './sass.ts';
import { esbuild, stopEsbuild } from './esbuild.ts';
import { sync } from './sync.ts';
import { hash } from './hash.ts';
import { stopManifest } from './manifest.ts';
import { env, errorMark, c } from './env.ts';
import { i18n } from './i18n.ts';
import { definedUnique } from './algo.ts';
import { clean } from './clean.ts';

export async function build(pkgs: string[]): Promise<void> {
  env.startTime = Date.now();
  try {
    try {
      chdir(env.rootDir);
      if (env.install) execSync('pnpm install', { stdio: 'inherit' });
      if (!pkgs.length) env.log(`Parsing packages in '${c.cyan(env.uiDir)}'`);

      await Promise.allSettled([parsePackages(), fs.promises.mkdir(env.buildTempDir)]);

      pkgs
        .filter(x => !env.packages.has(x))
        .forEach(x => env.exit(`${errorMark} - unknown package '${c.magenta(x)}'`));

      env.building =
        pkgs.length === 0 ? [...env.packages.values()] : definedUnique(pkgs.flatMap(p => env.deps(p)));

      if (pkgs.length) env.log(`Building ${c.grey(env.building.map(x => x.name).join(', '))}`);
    } finally {
      monitor(pkgs);
    }
    await Promise.all([i18n(), sync().then(hash).then(sass), tsc(), esbuild()]);
  } catch (e) {
    env.log(`${errorMark} ${e instanceof Error ? (e.stack ?? e.message) : String(e)}`);
    if (env.watch) env.log(c.grey('Watching...'));
    else env.exit();
  }
}

export function stopBuild(): Promise<any> {
  stopTask();
  stopSass();
  stopManifest(true);
  return Promise.allSettled([stopTsc(), stopEsbuild()]);
}

function monitor(pkgs: string[]) {
  if (!env.watch) return;
  return makeTask({
    key: 'monitor',
    includes: [
      { cwd: env.rootDir, path: 'package.json' },
      { cwd: env.typesDir, path: '*/package.json' },
      { cwd: env.uiDir, path: '*/package.json' },
      { cwd: env.typesDir, path: '*/*.d.ts' },
      { cwd: env.uiDir, path: '*/tsconfig.json' },
    ],
    debounce: 1000,
    monitorOnly: true,
    execute: async files => {
      if (files.some(x => x.endsWith('package.json'))) {
        if (!env.install) env.exit('Exiting due to package.json change');
        await stopBuild();
        if (env.clean) await clean();
        else await clean([relative(env.rootDir, join(env.buildTempDir, 'noCheck/*'))]);
        build(pkgs);
      } else if (files.some(x => x.endsWith('.d.ts') || x.endsWith('tsconfig.json'))) {
        stopManifest();
        await Promise.allSettled([stopTsc(), stopEsbuild()]);
        await clean([relative(env.rootDir, join(env.buildTempDir, 'noCheck/*'))]);
        tsc();
        esbuild();
      }
    },
  });
}
