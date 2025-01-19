import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { type Sync, globArray, globArrays } from './parse.ts';
import { updateManifest, manifests } from './manifest.ts';
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
    const sources = await globArrays(
      pkg.hash.map(x => x.glob),
      { cwd: env.outDir },
    );

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
  const hashed = (
    await Promise.all(
      env.building.flatMap(pkg =>
        pkg.hash.map(async hash =>
          (await globArray(hash.glob, { cwd: env.outDir })).map(path => ({
            path,
            replace: hash.replace,
            root: pkg.root,
          })),
        ),
      ),
    )
  ).flat();

  const sourceStats = await Promise.all(hashed.map(hash => fs.promises.stat(hash.path)));

  for (const [i, stat] of sourceStats.entries()) {
    const name = hashed[i].path.slice(env.outDir.length + 1);
    if (stat.mtimeMs === manifests.hashed[name]?.mtime) alreadyHashed.set(name, manifests.hashed[name].hash!);
    else newHashLinks.set(name, stat.mtimeMs);
  }
  await Promise.allSettled([...alreadyHashed].map(([name, hash]) => link(name, hash)));

  for await (const { name, hash } of [...newHashLinks.keys()].map(hashLink)) {
    manifests.hashed[name] = Object.defineProperty({ hash }, 'mtime', { value: newHashLinks.get(name) });
  }
  if (newHashLinks.size === 0 && alreadyHashed.size === Object.keys(manifests.hashed).length) return;

  for (const key of Object.keys(manifests.hashed)) {
    if (!hashed.some(x => x.path.endsWith(key))) delete manifests.hashed[key];
  }
  // TODO find a better home for all of this
  const replaceMany: Map<string, { root: string; mapping: Record<string, string> }> = new Map();
  for (const { root, path, replace } of hashed) {
    if (!replace) continue;
    const replaceInOne = replaceMany.get(replace) ?? { root, mapping: {} };
    const from = path.slice(env.outDir.length + 1);
    replaceInOne.mapping[from] = asHashed(from, manifests.hashed[from].hash!);
    replaceMany.set(replace, replaceInOne);
  }
  for await (const { name, hash } of [...replaceMany].map(([n, r]) => replaceAllIn(n, r.root, r.mapping))) {
    manifests.hashed[name] = { hash };
  }
  updateManifest({ dirty: true });
}

async function replaceAllIn(name: string, root: string, files: Record<string, string>) {
  const result = Object.entries(files).reduce(
    (data, [from, to]) => data.replaceAll(from, to),
    await fs.promises.readFile(path.join(root, name), 'utf8'),
  );
  const hash = crypto.createHash('sha256').update(result).digest('hex').slice(0, 8);
  await fs.promises.writeFile(path.join(env.hashOutDir, asHashed(name, hash)), result);
  return { name, hash };
}

async function hashLink(name: string) {
  const src = path.join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  await link(name, hash);
  return { name, hash };
}

async function link(name: string, hash: string) {
  const link = path.join(env.hashOutDir, asHashed(name, hash));
  return fs.promises.symlink(path.join('..', name), link).catch(() => {});
}

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.indexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}
