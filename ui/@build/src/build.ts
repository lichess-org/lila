import * as fs from 'node:fs';
import * as path from 'node:path';
import * as cps from 'node:child_process';
import * as ps from 'node:process';
import { parseModules } from './parse';
import { tsc } from './tsc';
import { sass } from './sass';
import { esbuild } from './esbuild';
import { LichessModule, env, errorMark, colors as c } from './main';

export let moduleDeps: Map<string, string[]>;
export let modules: Map<string, LichessModule>;
export let buildModules: LichessModule[];

export async function build(mods: string[]) {
  if (!mods.length) env.log(`Parsing modules in '${c.cyan(env.uiDir)}'`);

  ps.chdir(env.uiDir);

  [modules, moduleDeps] = await parseModules();

  if (mods.find(x => !known(x))) {
    env.log(`${errorMark} - unknown module '${c.magenta(mods.find(x => !known(x))!)}'`);
    return;
  }

  buildModules = mods.length === 0 ? [...modules.values()] : depsMany(mods);
  if (mods.length) {
    env.log(`Building ${c.grey(buildModules.map(x => x.name).join(', '))}`);
  }
  await fs.promises.mkdir(env.jsDir, { recursive: true });
  await fs.promises.mkdir(env.cssDir, { recursive: true });

  sass();
  tsc(() => esbuild());
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
  if (mod?.copy)
    for (const cp of mod.copy) {
      const sources: string[] = [];
      const dest = path.join(mod.root, cp.dest) + path.sep;
      if (typeof cp.src === 'string') {
        sources.push(path.join(mod.root, cp.src));
        env.log(`[${c.grey(mod.name)}] copy '${c.cyan(cp.src)}'`);
      } else if (Array.isArray(cp.src)) {
        for (const s of cp.src) {
          sources.push(path.join(mod.root, s));
          env.log(`[${c.grey(mod.name)}] copy '${c.cyan(s)}'`);
        }
      }
      fs.mkdirSync(dest, { recursive: true });

      cps.execFileSync('cp', ['-rf', ...sources, dest]);
    }
}

function depsOne(modName: string): LichessModule[] {
  const collect = (dep: string): string[] => [...(moduleDeps.get(dep) || []).flatMap(d => collect(d)), dep];
  return unique(collect(modName).map(name => modules.get(name)));
}

const depsMany = (modNames: string[]): LichessModule[] => unique(modNames.flatMap(depsOne));

const unique = <T>(mods: (T | undefined)[]): T[] => [...new Set(mods.filter(x => x))] as T[];

const known = (name: string): boolean => modules.has(name);
