import fg from 'fast-glob';
import crypto from 'node:crypto';
import fs from 'node:fs';
import { dirname, join, basename } from 'node:path';

import { env, type Package, type Hash } from './env.ts';

export async function parsePackages(): Promise<void> {
  const packageJsonPaths = await glob('ui/[^@.]*/package.json');
  const packages = await Promise.all(
    packageJsonPaths.map(packageJsonPath => parsePackage(dirname(packageJsonPath))),
  );

  for (const pkgInfo of packages) {
    env.packages.set(pkgInfo.name, pkgInfo);
  }

  for (const pkgInfo of env.packages.values()) {
    const workspaceDeps = Object.keys(pkgInfo.pkg.dependencies ?? {}).filter(dep => env.packages.has(dep));
    env.workspaceDeps.set(pkgInfo.name, workspaceDeps);
  }
}

export async function glob(glob?: string[] | string, opts: fg.Options = {}): Promise<string[]> {
  if (!glob) return [];
  const results = await Promise.all(
    [glob].flatMap(async g => fg.glob(g, { cwd: env.rootDir, absolute: true, ...opts })),
  );
  return [...new Set(results.flat())];
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
      (await fs.promises.readdir(folder).catch(() => [])).map(async f => {
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

export function getHash(content: string | Buffer) {
  return crypto.createHash('sha256').update(content).digest('hex').slice(0, 8);
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
  const normalize = (file: string) => (file.startsWith('/') ? file.slice(1) : join('ui', pkgInfo.name, file));
  const normalizeObject = <T extends Record<string, any>>(o: T) =>
    Object.fromEntries(Object.entries(o).map(([k, v]) => [k, typeof v === 'string' ? normalize(v) : v]));

  if ('hash' in build) {
    pkgInfo.hash = [build.hash]
      .flat()
      .map(g => (typeof g === 'string' ? { path: normalize(g) } : normalizeObject(g))) as Hash[];
  }

  if ('sync' in build)
    pkgInfo.sync = Object.entries<string>(build.sync).map(x => ({
      src: normalize(x[0]),
      dest: normalize(x[1]),
    }));

  if ('bundle' in build) {
    pkgInfo.bundle = [build.bundle].flat().map(b => (typeof b === 'string' ? { module: b } : b));
  }
  return pkgInfo;
}
