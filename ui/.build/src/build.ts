import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as ps from 'node:process';
import { parseModules } from './parse';
import { tsc, stopTsc } from './tsc';
import { sass, stopSass } from './sass';
import { esbuild, stopEsbuild } from './esbuild';
import { copies, stopCopies } from './copies';
import { startTickling, stopTickling } from './tickler';
import { clean } from './clean';
import { LichessModule, env, errorMark, colors as c } from './main';

export let moduleDeps: Map<string, string[]>;
export let modules: Map<string, LichessModule>;
export let buildModules: LichessModule[];

export async function build(mods: string[]) {
  await stop();
  await clean();

  if (env.install) cps.execSync('pnpm install', { cwd: env.rootDir, stdio: 'inherit' });
  if (!mods.length) env.log(`Parsing modules in '${c.cyan(env.uiDir)}'`);

  ps.chdir(env.uiDir);
  [modules, moduleDeps] = await parseModules();

  mods.filter(x => !modules.has(x)).forEach(x => env.exit(`${errorMark} - unknown module '${c.magenta(x)}'`));

  buildModules = mods.length === 0 ? [...modules.values()] : depsMany(mods);

  if (mods.length) env.log(`Building ${c.grey(buildModules.map(x => x.name).join(', '))}`);

  await Promise.allSettled([fs.promises.mkdir(env.jsDir), fs.promises.mkdir(env.cssDir)]);
  sass();
  await tsc();
  await copies();
  await esbuild();
  startTickling(mods);
}

export async function stop() {
  stopTickling();
  stopSass();
  stopTsc();
  stopCopies();
  await stopEsbuild();
}

export function postBuild() {
  for (const mod of buildModules) {
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
  const collect = (dep: string): string[] => [...(moduleDeps.get(dep) || []).flatMap(d => collect(d)), dep];
  return unique(collect(modName).map(name => modules.get(name)));
}

const depsMany = (modNames: string[]): LichessModule[] => unique(modNames.flatMap(depsOne));

const unique = <T>(mods: (T | undefined)[]): T[] => [...new Set(mods.filter(x => x))] as T[];
