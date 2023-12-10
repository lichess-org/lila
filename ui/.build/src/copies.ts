import * as fs from 'node:fs';
import * as path from 'node:path';
import { globArray } from './parse';
import { Copy, env, errorMark, colors as c } from './main';
import { buildModules } from './build';

const globRe = /[*?!{}[\]()]|\*\*|\[[^[\]]*\]/;
const copyWatch: fs.FSWatcher[] = [];
let watchTimeout: NodeJS.Timeout | undefined;

export function stopCopies() {
  clearTimeout(watchTimeout);
  watchTimeout = undefined;
  for (const watcher of copyWatch) watcher.close();
  copyWatch.length = 0;
}

export async function copies() {
  if (!env.copies) return;
  const watched = new Map<string, Copy[]>();
  const updated = new Set<string>();

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
        watchTimeout = setTimeout(fire, 2000);
      });
      watcher.on('error', (err: Error) => env.error(err));
      copyWatch.push(watcher);
    }
  }
}

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
  env.log(`[${c.grey(cp.mod.name)}] - Sync '${c.cyan(cp.src)}' to '${c.cyan(cp.dest)}'`);
  const fileCopies = [];

  for (const src of srcs) {
    const srcPath = path.join(cp.mod.root, src);
    watchDirs.add(path.dirname(srcPath));
    const destPath = path.join(dest, src.slice(globRoot.length));
    fileCopies.push(copyOne(srcPath, destPath, cp.mod.name));
  }
  await Promise.allSettled(fileCopies);
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

const quantize = (n?: number, factor = 10000) => Math.floor((n ?? 0) / factor) * factor;
