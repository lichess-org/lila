import fs from 'node:fs';
import path from 'node:path';
import fg from 'fast-glob';
import { env } from './env.ts';

export type Bundle = { module?: string; inline?: string };

export interface Package {
  root: string; // absolute path to package.json parentdir (package root)
  name: string; // dirname of package root
  pkg: any; // the entire package.json object
  bundle: { module?: string; inline?: string }[]; // TODO doc
  hashGlobs: string[]; // TODO doc
  sync: Sync[]; // pre-bundle filesystem copies from package json
}

export interface Sync {
  src: string; // src must be a file or a glob expression, use <dir>/** to sync entire directories
  dest: string; // TODO doc
  pkg: Package;
}

export async function parsePackages(): Promise<void> {
  for (const dir of (await globArray('[^@.]*/package.json')).map(pkg => path.dirname(pkg))) {
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

export async function globArray(glob: string, opts: fg.Options = {}): Promise<string[]> {
  const files: string[] = [];
  for await (const f of fg.stream(glob, { cwd: env.uiDir, absolute: true, onlyFiles: true, ...opts })) {
    files.push(f.toString('utf8'));
  }
  return files;
}

export async function globArrays(globs: string[] | undefined, opts: fg.Options = {}): Promise<string[]> {
  if (!globs) return [];
  const globResults = await Promise.all(globs.map(g => globArray(g, opts)));
  return [...new Set<string>(globResults.flat())];
}

export async function folderSize(folder: string): Promise<number> {
  let totalSize = 0;

  async function getSize(dir: string) {
    for (const file of await fs.promises.readdir(dir, { withFileTypes: true })) {
      if (file.isDirectory()) await getSize(path.join(dir, file.name));
      else if (file.isFile()) totalSize += (await fs.promises.stat(path.join(dir, file.name))).size;
    }
  }
  await getSize(folder);
  return totalSize;
}

export async function readable(file: string): Promise<boolean> {
  return fs.promises
    .access(file, fs.constants.R_OK)
    .then(() => true)
    .catch(() => false);
}

async function parsePackage(packageDir: string): Promise<Package> {
  const pkgInfo: Package = {
    pkg: JSON.parse(await fs.promises.readFile(path.join(packageDir, 'package.json'), 'utf8')),
    name: path.basename(packageDir),
    root: packageDir,
    bundle: [],
    sync: [],
    hashGlobs: [],
  };
  if (!('build' in pkgInfo.pkg)) return pkgInfo;
  const build = pkgInfo.pkg.build;

  if ('hash' in build) pkgInfo.hashGlobs = [].concat(build.hash);

  if ('bundle' in build) {
    for (const one of [].concat(build.bundle).map<Bundle>(b => (typeof b === 'string' ? { module: b } : b))) {
      if (!one.module) continue;

      if (await readable(path.join(pkgInfo.root, one.module))) pkgInfo.bundle.push(one);
      else
        pkgInfo.bundle.push(
          ...(await globArray(one.module, { cwd: pkgInfo.root, absolute: false })).map(module => ({
            ...one,
            module,
          })),
        );
    }
  }
  if ('sync' in build) {
    pkgInfo.sync = Object.entries<string>(build.sync).map(x => ({
      src: x[0],
      dest: x[1],
      pkg: pkgInfo,
    }));
  }
  return pkgInfo;
}
