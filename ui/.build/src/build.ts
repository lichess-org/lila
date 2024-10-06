import fs from 'node:fs';
import cps from 'node:child_process';
import ps from 'node:process';
import { parsePackages } from './parse.ts';
import { tsc, stopTsc } from './tsc.ts';
import { sass, stopSass } from './sass.ts';
import { esbuild, stopEsbuild } from './esbuild.ts';
import { copies, stopCopies } from './copies.ts';
import { monitor, stopMonitor } from './monitor.ts';
import { writeManifest } from './manifest.ts';
import { clean } from './clean.ts';
import { type Package, env, errorMark, colors as c } from './main.ts';

export async function build(pkgs: string[]): Promise<void> {
  await stop();
  await clean();

  if (env.install) cps.execSync('pnpm install', { cwd: env.rootDir, stdio: 'inherit' });
  if (!pkgs.length) env.log(`Parsing packages in '${c.cyan(env.uiDir)}'`);

  ps.chdir(env.uiDir);
  [env.packages, env.deps] = await parsePackages();

  pkgs
    .filter(x => !env.packages.has(x))
    .forEach(x => env.exit(`${errorMark} - unknown package '${c.magenta(x)}'`));

  env.building = pkgs.length === 0 ? [...env.packages.values()] : depsMany(pkgs);

  if (pkgs.length) env.log(`Building ${c.grey(env.building.map(x => x.name).join(', '))}`);

  await Promise.allSettled([
    fs.promises.mkdir(env.jsOutDir),
    fs.promises.mkdir(env.cssOutDir),
    fs.promises.mkdir(env.hashOutDir),
    fs.promises.mkdir(env.themeGenDir),
    fs.promises.mkdir(env.buildTempDir),
  ]);

  monitor(pkgs);
  await Promise.all([sass(), copies(), esbuild(tsc())]);
}

export async function stop(): Promise<void> {
  stopMonitor();
  stopSass();
  stopTsc();
  stopCopies();
  await stopEsbuild();
}

export function postBuild(): void {
  writeManifest();
  for (const pkg of env.building) {
    pkg.post.forEach((args: string[]) => {
      env.log(`[${c.grey(pkg.name)}] exec - ${c.cyanBold(args.join(' '))}`);
      const stdout = cps.execSync(`${args.join(' ')}`, { cwd: pkg.root });
      if (stdout) env.log(stdout, { ctx: pkg.name });
    });
  }
}

export function prePackage(pkg: Package | undefined): void {
  pkg?.pre.forEach((args: string[]) => {
    env.log(`[${c.grey(pkg.name)}] exec - ${c.cyanBold(args.join(' '))}`);
    const stdout = cps.execSync(`${args.join(' ')}`, { cwd: pkg.root });
    if (stdout) env.log(stdout, { ctx: pkg.name });
  });
}

function depsOne(pkgName: string): Package[] {
  const collect = (dep: string): string[] => [...(env.deps.get(dep) || []).flatMap(d => collect(d)), dep];
  return unique(collect(pkgName).map(name => env.packages.get(name)));
}

const depsMany = (pkgNames: string[]): Package[] => unique(pkgNames.flatMap(depsOne));

const unique = <T>(pkgs: (T | undefined)[]): T[] => [...new Set(pkgs.filter(x => x))] as T[];
