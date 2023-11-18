import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as ps from 'node:process';
import { parseModules } from './parse';
import { tsc, killTsc } from './tsc';
import { sass, killSass } from './sass';
import { esbuild, killEsbuild } from './esbuild';
import { copies, killCopies } from './copies';
import { clean } from './clean';
import { LichessModule, env, errorMark, colors as c } from './main';

export let moduleDeps: Map<string, string[]>;
export let modules: Map<string, LichessModule>;
export let buildModules: LichessModule[];
const pkgWatch: fs.FSWatcher[] = [];
let rebuildTimeout: NodeJS.Timeout | undefined;

export async function build(mods: string[], install = false) {
  rebuildTimeout = undefined;
  killSass();
  killTsc();
  killCopies();
  await killEsbuild();
  await clean();

  if (install || env.install) cps.execSync('pnpm install', { cwd: env.rootDir, stdio: 'inherit' });
  ps.chdir(env.uiDir);

  if (!mods.length) env.log(`Parsing modules in '${c.cyan(env.uiDir)}'`);
  [modules, moduleDeps] = await parseModules();

  if (mods.find(x => !known(x))) {
    env.log(`${errorMark} - unknown module '${c.magenta(mods.find(x => !known(x))!)}'`);
    return;
  }

  buildModules = mods.length === 0 ? [...modules.values()] : depsMany(mods);
  if (mods.length) env.log(`Building ${c.grey(buildModules.map(x => x.name).join(', '))}`);

  await fs.promises.mkdir(env.jsDir, { recursive: true });
  await fs.promises.mkdir(env.cssDir, { recursive: true });
  packageWatch(mods);
  sass();
  await tsc();
  await copies();
  await esbuild();
}

function packageWatch(mods: string[]) {
  if (!env.watch) return;

  clearTimeout(rebuildTimeout);
  for (const pkg of pkgWatch) pkg.close();
  pkgWatch.length = 0;

  const notify = () => {
    clearTimeout(rebuildTimeout);
    if (env.rebuild) rebuildTimeout = setTimeout(() => build(mods, true), 2000);
    else {
      env.warn('Exiting due to package.json change');
      env.warn('Use --rebuild or -r to reinstall & rebuild rather than exit');
      ps.exit(0);
    }
  };
  pkgWatch.push(fs.watch(path.join(env.rootDir, 'package.json'), notify));
  for (const mod of buildModules) {
    pkgWatch.push(fs.watch(path.join(mod.root, 'package.json'), notify));
    pkgWatch.push(fs.watch(path.join(mod.root, 'tsconfig.json'), notify));
  }
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

const known = (name: string): boolean => modules.has(name);
