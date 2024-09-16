import fs from 'node:fs';
import path from 'node:path';
import fg from 'fast-glob';
import { Package, env } from './main';

export async function parsePackages(): Promise<[Map<string, Package>, Map<string, string[]>]> {
  const packages = new Map<string, Package>();
  const packageDeps = new Map<string, string[]>();

  for (const dir of (await globArray('[^@.]*/package.json')).map(pkg => path.dirname(pkg))) {
    const pkgInfo = await parsePackage(dir);
    packages.set(pkgInfo.name, pkgInfo);
  }

  for (const pkgInfo of packages.values()) {
    const deplist: string[] = [];
    for (const dep in pkgInfo.pkg.dependencies) {
      if (packages.has(dep)) deplist.push(dep);
    }
    packageDeps.set(pkgInfo.name, deplist);
  }
  return [packages, packageDeps];
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

async function parsePackage(packageDir: string): Promise<Package> {
  const pkg = JSON.parse(await fs.promises.readFile(path.join(packageDir, 'package.json'), 'utf8'));
  const pkgInfo: Package = {
    pkg,
    name: path.basename(packageDir),
    root: packageDir,
    pre: [],
    post: [],
  };

  if ('lichess' in pkg && 'hashed' in pkg.lichess) pkgInfo.hashGlobs = pkg.lichess.hashed as string[];

  if ('lichess' in pkg && 'bundles' in pkg.lichess) {
    if (typeof pkg.lichess.bundles === 'string') pkgInfo.bundles = [pkg.lichess.bundles];
    else pkgInfo.bundles = pkg.lichess.bundles as string[];
  }
  if ('lichess' in pkg && 'sync' in pkg.lichess) {
    pkgInfo.sync = Object.entries(pkg.lichess.sync).map(x => ({
      src: x[0],
      dest: x[1] as string,
      pkg: pkgInfo,
    }));
  }
  return pkgInfo;
}
