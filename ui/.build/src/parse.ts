import fs from 'node:fs';
import { dirname, join, basename } from 'node:path';
import fg from 'fast-glob';
import { env } from './env.ts';

export interface Package {
  root: string; // absolute path to package.json parentdir
  name: string; // dirname of package root
  pkg: any; // package.json object
  bundle: Bundle[]; // esbuild bundling
  hash: Hash[]; // files to symlink hash
  sync: Sync[]; // pre-bundle filesystem copies from package json
}

interface Bundle {
  module?: string; // file glob for esm modules (esbuild entry points)
  inline?: string; // inject this script into response html
}

interface Hash {
  path: string; // glob for assets
  catalog?: string; // file to update with hashed filenames
  omit?: boolean; // omit from client manifest, default false
}

interface Sync {
  src: string; // file glob expression, use <dir>/** to sync entire directories
  dest: string; // directory to copy into
}

export async function parsePackages(): Promise<void> {
  for (const dir of (await glob('ui/[^@.]*/package.json')).map(pkg => dirname(pkg))) {
    const pkgInfo = await parsePackage(dir);
    env.packages.set(pkgInfo.name, pkgInfo);
  }

  for (const pkgInfo of env.packages.values()) {
    const deplist: string[] = [];
    for (const dep in pkgInfo.pkg.dependencies) {
      if (env.packages.has(dep)) deplist.push(dep);
    }
    env.workspaceDeps.set(pkgInfo.name, deplist);
  }
}

export async function glob(glob: string[] | string | undefined, opts: fg.Options = {}): Promise<string[]> {
  if (!glob) return [];
  const results = await Promise.all(
    Array()
      .concat(glob)
      .map(async g => fg.glob(g, { cwd: env.rootDir, absolute: true, ...opts })),
  );
  return [...new Set(results.flat())];
}

export async function folderSize(folder: string): Promise<number> {
  async function getSize(dir: string): Promise<number> {
    const entries = await fs.promises.readdir(dir, { withFileTypes: true });

    const sizes = await Promise.all(
      entries.map(async entry => {
        const fullPath = join(dir, entry.name);
        if (entry.isDirectory()) return getSize(fullPath);
        if (entry.isFile()) return (await fs.promises.stat(fullPath)).size;
        return 0;
      }),
    );
    return sizes.reduce((acc: number, size: number) => acc + size, 0);
  }
  return getSize(folder);
}

export async function readable(file: string): Promise<boolean> {
  return fs.promises
    .access(file, fs.constants.R_OK)
    .then(() => true)
    .catch(() => false);
}

export async function subfolders(folder: string, depth = 1): Promise<string[]> {
  if (depth <= 0) return [];
  return (
    await Promise.all(
      (
        await fs.promises.readdir(folder).catch(() => [])
      ).map(async f => {
        const fullpath = join(folder, f);
        return (await isFolder(fullpath)) ? [fullpath, ...(await subfolders(fullpath, depth - 1))] : [];
      }),
    )
  ).flat();
}

export function isFolder(file: string): Promise<boolean> {
  return fs.promises
    .stat(file)
    .then(s => s.isDirectory())
    .catch(() => false);
}

export function isGlob(path: string): boolean {
  return /[*?!{}[\]()]/.test(path);
}

export function isClose(a: number | undefined, b: number | undefined, epsilon = 2) {
  return a === b || Math.abs((a ?? NaN) - (b ?? NaN)) < epsilon; // for mtimeMs jitter
}

async function parsePackage(root: string): Promise<Package> {
  const pkgInfo: Package = {
    pkg: JSON.parse(await fs.promises.readFile(join(root, 'package.json'), 'utf8')),
    name: basename(root),
    root,
    bundle: [],
    sync: [],
    hash: [],
  };
  if (!('build' in pkgInfo.pkg)) return pkgInfo;
  const build = pkgInfo.pkg.build;

  // 'hash' and 'sync' paths beginning with '/' are repo relative, otherwise they are package relative
  const normalize = (file: string) => (file[0] === '/' ? file.slice(1) : join('ui', pkgInfo.name, file));
  const normalizeObject = <T extends Record<string, any>>(o: T) =>
    Object.fromEntries(Object.entries(o).map(([k, v]) => [k, typeof v === 'string' ? normalize(v) : v]));

  if ('hash' in build)
    pkgInfo.hash = []
      .concat(build.hash)
      .map(g => (typeof g === 'string' ? { path: normalize(g) } : normalizeObject(g))) as Hash[];

  if ('sync' in build)
    pkgInfo.sync = Object.entries<string>(build.sync).map(x => ({
      src: normalize(x[0]),
      dest: normalize(x[1]),
    }));

  if ('bundle' in build)
    pkgInfo.bundle = [].concat(build.bundle).map(b => (typeof b === 'string' ? { module: b } : b));
  return pkgInfo;
}
