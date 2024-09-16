import fs from 'node:fs';
import path from 'node:path';
import { globArray, globArrays } from './parse';
import { hashedManifest, writeManifest } from './manifest';
import { Sync, env, errorMark, colors as c } from './main';

const syncWatch: fs.FSWatcher[] = [];
let watchTimeout: NodeJS.Timeout | undefined;

export function stopCopies(): void {
  clearTimeout(watchTimeout);
  watchTimeout = undefined;
  for (const watcher of syncWatch) watcher.close();
  syncWatch.length = 0;
}

export async function copies(): Promise<void> {
  if (!env.copies) return;
  const watched = new Map<string, Sync[]>();
  const updated = new Set<string>();

  for (const pkg of env.building) {
    for (const cp of pkg.sync ?? []) {
      for (const src of await globSync(cp)) {
        if (env.watch) watched.set(src, [...(watched.get(src) ?? []), cp]);
      }
    }
    if (!env.watch) continue;
    const sources = await globArrays(pkg.hashGlobs, { cwd: env.outDir });

    for (const src of sources.filter(isUnmanagedAsset)) {
      if (!watched.has(path.dirname(src))) watched.set(path.dirname(src), []);
    }
  }
  if (env.watch)
    for (const dir of watched.keys()) {
      const watcher = fs.watch(dir);

      watcher.on('change', () => {
        updated.add(dir);
        clearTimeout(watchTimeout);

        watchTimeout = setTimeout(() => {
          Promise.all([...updated].flatMap(d => (watched.get(d) ?? []).map(x => globSync(x)))).then(() => {
            hashedManifest();
            writeManifest();
          });
          updated.clear();
          watchTimeout = undefined;
        }, 2000);
      });
      watcher.on('error', (err: Error) => env.error(err));
      syncWatch.push(watcher);
    }
  hashedManifest();
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
    if (src && (!dest || quantize(src.mtimeMs) !== quantize(dest.mtimeMs))) {
      await fs.promises.copyFile(absSrc, absDest);
      fs.utimes(absDest, src.atime, src.mtime, () => {});
    }
  } catch (_) {
    env.log(`[${c.grey(pkgName)}] - ${errorMark} - failed sync '${c.cyan(absSrc)}' to '${c.cyan(absDest)}'`);
  }
}

const quantize = (n?: number, factor = 10000) => Math.floor((n ?? 0) / factor) * factor;
