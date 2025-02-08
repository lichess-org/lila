import fs from 'node:fs';
import p from 'node:path';
import crypto from 'node:crypto';
import { task } from './task.ts';
import { type Manifest, updateManifest } from './manifest.ts';
import { env, c } from './env.ts';
import { type Package, isClose } from './parse.ts';
import { isEquivalent } from './algo.ts';

export async function hash(): Promise<void> {
  if (!env.begin('hash')) return;
  const hashed: Manifest = {};
  const pathOnly: { glob: string[] } = { glob: [] };
  const hashRuns: { glob: string | string[]; update?: string; pkg?: Package }[] = [];

  for (const [pkg, { glob, update, ...rest }] of env.tasks('hash')) {
    update ? hashRuns.push({ glob, update, pkg, ...rest }) : pathOnly.glob.push(glob);
    hashLog(glob, '', pkg?.name);
  }
  if (pathOnly.glob.length) hashRuns.push(pathOnly);

  await fs.promises.mkdir(env.hashOutDir).catch(() => {});
  const freshHashes = await freshSymlinkHashes();
  await Promise.all(
    hashRuns.map(({ glob, update, pkg }) =>
      task({
        pkg,
        ctx: 'hash',
        debounce: 300,
        root: env.rootDir,
        glob: Array<string>()
          .concat(glob)
          .map(path => ({ cwd: env.rootDir, path })),
        execute: async (files, fullList) => {
          const shouldLog = !isEquivalent(files, fullList);
          await Promise.all(
            files.map(async src => {
              const name = p.relative(env.outDir, src);
              const hash =
                freshHashes[name] && !(await isLinkStale(hashedBasename(name, freshHashes[name])))
                  ? freshHashes[name]
                  : await hashLink(name);
              hashed[name] = { hash };
              if (shouldLog) hashLog(src, hashedBasename(name, hash), pkg?.name);
            }),
          );
          if (update && pkg?.root) {
            const updates: Record<string, string> = {};
            for (const src of fullList.map(f => p.relative(env.outDir, f))) {
              updates[src] = hashedBasename(src, hashed[src].hash!);
            }
            const { name, hash } = await replaceHash(p.relative(pkg.root, update), pkg.root, updates);
            hashed[name] = { hash };
            if (shouldLog) hashLog(name, hashedBasename(name, hash), pkg.name);
          }
          updateManifest({ hashed });
        },
      }),
    ),
  );
}

export async function freshSymlinkHashes(newLinks?: string[]) {
  const freshHashes = {} as Record<string, string>;
  if (newLinks && newLinks.length === 0) return freshHashes;

  await fs.promises.readdir(env.hashOutDir).then(files =>
    Promise.all(
      files.map(async symlink => {
        const [, hash] = symlink.match(/^.+\.([0-9a-f]{8})(?:\.[^\.]*)?$/) ?? [];
        if (!hash || (newLinks && !newLinks.some(l => l.endsWith(symlink)))) return;
        const absSymlink = p.join(env.hashOutDir, symlink);
        try {
          const [target, stale] = await Promise.all([fs.promises.readlink(absSymlink), isLinkStale(symlink)]);
          if (!stale) freshHashes[p.relative(env.outDir, p.resolve(env.hashOutDir, target))] = hash;
        } catch {} // ignore all entries that are not valid symlinks
      }),
    ),
  );
  return freshHashes;
}

export function hashedBasename(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf(p.sep) + 1);
  const extPos = name.lastIndexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}

async function isLinkStale(symlink: string | undefined) {
  if (!symlink) return true;
  const absSymlink = p.join(env.hashOutDir, symlink);
  const [{ mtimeMs: linkMs }, { mtimeMs: targetMs }] = await Promise.all([
    fs.promises.lstat(absSymlink),
    fs.promises.stat(absSymlink),
  ]);
  return !isClose(linkMs, targetMs);
}

async function replaceHash(name: string, root: string, files: Record<string, string>) {
  const result = Object.entries(files).reduce(
    (data, [from, to]) => data.replaceAll(from, to),
    await fs.promises.readFile(p.join(root, name), 'utf8'),
  );
  const hash = crypto.createHash('sha256').update(result).digest('hex').slice(0, 8);
  await fs.promises.writeFile(p.join(env.hashOutDir, hashedBasename(name, hash)), result);
  return { name, hash };
}

async function hashLink(name: string) {
  const src = p.join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  const link = p.join(env.hashOutDir, hashedBasename(name, hash));
  const [{ mtime }] = await Promise.all([
    fs.promises.stat(p.join(env.outDir, name)),
    fs.promises.symlink(p.relative(env.outDir, name), link).catch(() => {}),
  ]);
  await fs.promises.lutimes(link, mtime, mtime);
  return hash;
}

function hashLog(src: string, hashName: string, pkgName?: string) {
  env.log(
    `${pkgName ? c.grey(pkgName) + ' ' : ''}'${c.cyan(src)}' -> '${c.cyan(p.join('public', 'hashed', hashName))}'`,
    'hash',
  );
}
