import fs from 'node:fs';
import path from 'node:path';
import fg from 'fast-glob';
import { env, errorMark, colors as c } from './env.ts';

export type Bundle = { module?: string; inline?: string };

export interface Package {
  root: string; // absolute path to package.json parentdir (package root)
  name: string; // dirname of package root
  pkg: any; // the entire package.json object
  bundle: { module?: string; inline?: string }[]; // TODO doc
  hash: { glob: string; replace?: string }[]; // TODO doc
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
  async function getSize(dir: string): Promise<number> {
    const entries = await fs.promises.readdir(dir, { withFileTypes: true });

    const sizes = await Promise.all(
      entries.map(async entry => {
        const fullPath = path.join(dir, entry.name);
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

async function parsePackage(packageDir: string): Promise<Package> {
  const pkgInfo: Package = {
    pkg: JSON.parse(await fs.promises.readFile(path.join(packageDir, 'package.json'), 'utf8')),
    name: path.basename(packageDir),
    root: packageDir,
    bundle: [],
    sync: [],
    hash: [],
  };
  if (!('build' in pkgInfo.pkg)) return pkgInfo;
  const build = pkgInfo.pkg.build;

  if ('hash' in build)
    pkgInfo.hash = [].concat(build.hash).map(glob => (typeof glob === 'string' ? { glob } : glob));

  if ('bundle' in build) {
    for (const one of [].concat(build.bundle).map<Bundle>(b => (typeof b === 'string' ? { module: b } : b))) {
      const src = one.module ?? one.inline;
      if (!src) continue;

      if (await readable(path.join(pkgInfo.root, src))) pkgInfo.bundle.push(one);
      else if (one.module)
        pkgInfo.bundle.push(
          ...(await globArray(one.module, { cwd: pkgInfo.root, absolute: false }))
            .filter(m => !m.endsWith('.inline.ts')) // no globbed inline sources
            .map(module => ({ ...one, module })),
        );
      else env.log(`[${c.grey(pkgInfo.name)}] - ${errorMark} - Bundle error ${c.blue(JSON.stringify(one))}`);
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

export function trimAndConsolidateWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}
