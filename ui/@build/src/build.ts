import * as fs from 'node:fs';
import * as path from 'node:path';
import * as cps from 'node:child_process';
import * as ps from 'node:process';
import { parseModules, globArray } from './parse';
import { tsc } from './tsc';
import { sass } from './sass';
import { esbuild } from './esbuild';
import { LichessModule, Copy, env, errorMark, colors as c } from './main';

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
  await tsc();
  await copies();
  await esbuild();
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

export async function copies() {
  const watched = new Map<string, Copy[]>();
  const updated = new Set<string>();
  let watchTimeout: NodeJS.Timeout | undefined;
  const fire = () => {
    updated.forEach(d => watched.get(d)?.forEach(globCopy));
    updated.clear();
    watchTimeout = undefined;
  };
  for (const mod of buildModules) {
    if (!mod?.copy) continue;
    for (const cp of mod.copy) {
      for (const src of await globCopy(cp)) {
        watched.set(src, [...(watched.get(src) ?? []), cp]);
      }
    }
    if (!env.watch) continue;
    for (const dir of watched.keys()) {
      const watcher = fs.watch(dir);
      watcher.on('change', () => {
        updated.add(dir);
        clearTimeout(watchTimeout);
        watchTimeout = setTimeout(fire, 600);
      });
      watcher.on('error', (err: Error) => env.error(err));
    }
  }
}

const globRe = /[*?!{}[\]()]|\*\*|\[[^[\]]*\]/;

async function globCopy(cp: Copy): Promise<Set<string>> {
  const watchDirs = new Set<string>();
  const dest = path.join(cp.mod.root, cp.dest) + path.sep;

  const globIndex = cp.src.search(globRe);
  const globRoot =
    globIndex > 0 && cp.src[globIndex - 1] === path.sep
      ? cp.src.slice(0, globIndex - 1)
      : path.dirname(cp.src.slice(0, globIndex));

  const srcs = await globArray(cp.src, { cwd: cp.mod.root, abs: false });

  watchDirs.add(path.join(cp.mod.root, globRoot));
  env.log(`[${c.grey(cp.mod.name)}] - Copy '${c.cyan(cp.src)}' to '${c.cyan(cp.dest)}'`);
  for (const src of srcs) {
    const srcPath = path.join(cp.mod.root, src);
    watchDirs.add(path.dirname(srcPath));
    const destPath = path.join(dest, src.slice(globRoot.length));
    await copyOne(srcPath, destPath, cp.mod.name);
  }
  return watchDirs;
}

async function copyOne(absSrc: string, absDest: string, modName: string) {
  try {
    const [src, dest] = (
      await Promise.allSettled([
        fs.promises.stat(absSrc),
        fs.promises.stat(absDest),
        fs.promises.mkdir(path.dirname(absDest), { recursive: true }),
      ])
    ).map(x => (x.status === 'fulfilled' ? (x.value as fs.Stats) : undefined));
    if (src && (!dest || quantize(src.mtimeMs) !== quantize(dest.mtimeMs))) {
      await fs.promises.copyFile(absSrc, absDest);
      fs.utimes(absDest, src.atime, src.mtime, () => {});
    }
  } catch (_) {
    env.log(`[${c.grey(modName)}] - ${errorMark} - failed copy '${c.cyan(absSrc)}' to '${c.cyan(absDest)}'`);
  }
}

function depsOne(modName: string): LichessModule[] {
  const collect = (dep: string): string[] => [...(moduleDeps.get(dep) || []).flatMap(d => collect(d)), dep];
  return unique(collect(modName).map(name => modules.get(name)));
}

const quantize = (n?: number, factor = 10000) => Math.floor((n ?? 0) / factor) * factor;

const depsMany = (modNames: string[]): LichessModule[] => unique(modNames.flatMap(depsOne));

const unique = <T>(mods: (T | undefined)[]): T[] => [...new Set(mods.filter(x => x))] as T[];

const known = (name: string): boolean => modules.has(name);
