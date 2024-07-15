import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as ps from 'node:process';
import { parseModules } from './parse';
import { tsc, stopTsc } from './tsc';
import { sass, stopSass } from './sass';
import { esbuild, stopEsbuild } from './esbuild';
import { copies, stopCopies } from './copies';
import { startMonitor, stopMonitor } from './monitor';
import { writeManifest } from './manifest';
import { clean } from './clean';
import { LichessModule, env, errorMark, colors as c } from './main';

export async function build(mods: string[]) {
  await stop();
  await clean();

  if (env.install) cps.execSync('pnpm install', { cwd: env.rootDir, stdio: 'inherit' });
  if (!mods.length) env.log(`Parsing modules in '${c.cyan(env.uiDir)}'`);

  ps.chdir(env.uiDir);
  [env.modules, env.deps] = await parseModules();

  mods
    .filter(x => !env.modules.has(x))
    .forEach(x => env.exit(`${errorMark} - unknown module '${c.magenta(x)}'`));

  env.building = mods.length === 0 ? [...env.modules.values()] : depsMany(mods);

  if (mods.length) env.log(`Building ${c.grey(env.building.map(x => x.name).join(', '))}`);

  await Promise.allSettled([
    fs.promises.mkdir(env.jsDir),
    fs.promises.mkdir(env.cssDir),
    fs.promises.mkdir(env.hashDir),
    fs.promises.mkdir(env.themeGenDir),
    fs.promises.mkdir(env.cssTempDir),
  ]);

  startMonitor(mods);
  await Promise.all([sass(), copies(), esbuild(tsc())]);
}

export async function stop() {
  stopMonitor();
  stopSass();
  stopTsc();
  stopCopies();
  await stopEsbuild();
}

export function postBuild() {
  writeManifest();
  for (const mod of env.building) {
    mod.post.forEach((args: string[]) => {
      env.log(`[${c.grey(mod.name)}] exec - ${c.cyanBold(args.join(' '))}`);
      const stdout = cps.execSync(`${args.join(' ')}`, { cwd: mod.root });
      if (stdout) env.log(stdout, { ctx: mod.name });
    });
  }
}

export function preModule(mod: LichessModule | undefined) {
  mod?.pre.forEach((args: string[]) => {
    env.log(`[${c.grey(mod.name)}] exec - ${c.cyanBold(args.join(' '))}`);
    const stdout = cps.execSync(`${args.join(' ')}`, { cwd: mod.root });
    if (stdout) env.log(stdout, { ctx: mod.name });
  });
}

function depsOne(modName: string): LichessModule[] {
  const collect = (dep: string): string[] => [...(env.deps.get(dep) || []).flatMap(d => collect(d)), dep];
  return unique(collect(modName).map(name => env.modules.get(name)));
}

const depsMany = (modNames: string[]): LichessModule[] => unique(modNames.flatMap(depsOne));

const unique = <T>(mods: (T | undefined)[]): T[] => [...new Set(mods.filter(x => x))] as T[];
