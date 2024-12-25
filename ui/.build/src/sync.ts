import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { type Sync, globArray, globArrays } from './parse.ts';
import { updateManifest, current } from './manifest.ts';
import { env, errorMark, colors as c } from './env.ts';
import { quantize } from './algo.ts';

const syncWatch: fs.FSWatcher[] = [];
let watchTimeout: NodeJS.Timeout | undefined;

export function stopSync(): void {
  clearTimeout(watchTimeout);
  for (const watcher of syncWatch) watcher.close();
  syncWatch.length = 0;
}

export async function sync(): Promise<void> {
  if (!env.sync) return;
  const watched = new Map<string, Sync[]>();
  const updated = new Set<string>();

  for (const pkg of env.building) {
    for (const sync of pkg.sync) {
      for (const src of await globSync(sync)) {
        if (env.watch) watched.set(src, [...(watched.get(src) ?? []), sync]);
      }
    }
    if (!env.watch) continue;
    const sources = await globArrays(pkg.hashGlobs, { cwd: env.outDir });

    for (const src of sources.filter(isUnmanagedAsset)) {
      if (!watched.has(path.dirname(src))) watched.set(path.dirname(src), []);
    }
  }
  hashedManifest();
  if (env.watch)
    for (const dir of watched.keys()) {
      const watcher = fs.watch(dir);

      watcher.on('change', () => {
        updated.add(dir);
        clearTimeout(watchTimeout);
        watchTimeout = setTimeout(() => {
          Promise.all([...updated].flatMap(d => (watched.get(d) ?? []).map(x => globSync(x)))).then(
            hashedManifest,
          );
          updated.clear();
        }, 2000);
      });
      watcher.on('error', (err: Error) => env.error(err));
      syncWatch.push(watcher);
    }
}

export function isUnmanagedAsset(absfile: string): boolean {
  if (!absfile.startsWith(env.outDir)) return false;
  const name = absfile.slice(env.outDir.length + 1);
  if (['compiled/', 'hashed/', 'css/'].some(dir => name.startsWith(dir))) return false;
  return true;
}

async function globSync(cp: Sync): Promise<Set<string>> {
  const watchDirs = new Set<string>();
  const dest = path.join(env.rootDir, cp.dest) + path.sep;

  const globIndex = cp.src.search(/[*?!{}[\]()]|\*\*|\[[^[\]]*\]/);
  const globRoot =
    globIndex > 0 && cp.src[globIndex - 1] === path.sep
      ? cp.src.slice(0, globIndex - 1)
      : path.dirname(cp.src.slice(0, globIndex));

  const srcs = await globArray(cp.src, { cwd: cp.pkg.root, absolute: false });

  watchDirs.add(path.join(cp.pkg.root, globRoot));
  env.log(`[${c.grey(cp.pkg.name)}] - Sync '${c.cyan(cp.src)}' to '${c.cyan(cp.dest)}'`);
  const fileCopies = [];

  for (const src of srcs) {
    const srcPath = path.join(cp.pkg.root, src);
    watchDirs.add(path.dirname(srcPath));
    const destPath = path.join(dest, src.slice(globRoot.length));
    fileCopies.push(syncOne(srcPath, destPath, cp.pkg.name));
  }
  await Promise.allSettled(fileCopies);
  return watchDirs;
}

async function syncOne(absSrc: string, absDest: string, pkgName: string) {
  try {
    const [src, dest] = (
      await Promise.allSettled([
        fs.promises.stat(absSrc),
        fs.promises.stat(absDest),
        fs.promises.mkdir(path.dirname(absDest), { recursive: true }),
      ])
    ).map(x => (x.status === 'fulfilled' ? (x.value as fs.Stats) : undefined));
    if (src && (!dest || quantize(src.mtimeMs, 2000) !== quantize(dest.mtimeMs, 2000))) {
      await fs.promises.copyFile(absSrc, absDest);
      fs.utimes(absDest, src.atime, src.mtime, () => {});
    }
  } catch (_) {
    env.log(`[${c.grey(pkgName)}] - ${errorMark} - failed sync '${c.cyan(absSrc)}' to '${c.cyan(absDest)}'`);
  }
}

async function hashedManifest(): Promise<void> {
  const newHashLinks = new Map<string, number>();
  const alreadyHashed = new Map<string, string>();
  const sources: string[] = await globArrays(
    env.building.flatMap(x => x.hashGlobs),
    { cwd: env.outDir },
  );
  const sourceStats = await Promise.all(sources.map(file => fs.promises.stat(file)));

  for (const [i, stat] of sourceStats.entries()) {
    const name = sources[i].slice(env.outDir.length + 1);

    if (stat.mtimeMs === current.hashed[name]?.mtime) alreadyHashed.set(name, current.hashed[name].hash!);
    else newHashLinks.set(name, stat.mtimeMs);
  }
  await Promise.allSettled([...alreadyHashed].map(([name, hash]) => link(name, hash)));

  for (const { name, hash } of await Promise.all([...newHashLinks.keys()].map(hashLink))) {
    current.hashed[name] = Object.defineProperty({ hash }, 'mtime', { value: newHashLinks.get(name) });
  }

  if (newHashLinks.size === 0 && alreadyHashed.size === Object.keys(current.hashed).length) return;

  for (const name of Object.keys(current.hashed)) {
    if (!sources.some(x => x.endsWith(name))) delete current.hashed[name];
  }
  updateManifest({ dirty: true });
}

async function hashLink(name: string) {
  const src = path.join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  link(name, hash);
  return { name, hash };
}

function link(name: string, hash: string) {
  const link = path.join(env.hashOutDir, asHashed(name, hash));
  fs.promises.symlink(path.join('..', name), link).catch(() => {});
}

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.indexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}
