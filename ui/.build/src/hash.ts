import fs from 'node:fs';
import crypto from 'node:crypto';
import { relative, join, resolve } from 'node:path';
import { makeTask } from './task.ts';
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
  const symlinkHashes = await symlinkTargetHashes();
  await Promise.all(
    hashRuns.map(({ glob, update, pkg }) =>
      makeTask({
        pkg,
        ctx: 'hash',
        debounce: 300,
        root: env.rootDir,
        includes: Array<string>()
          .concat(glob)
          .map(path => ({ cwd: env.rootDir, path })),
        execute: async (files, fullList) => {
          const shouldLog = !isEquivalent(files, fullList);
          await Promise.all(
            files.map(async src => {
              const name = relative(env.outDir, src);
              const hash =
                symlinkHashes[name] && !(await isLinkStale(hashedBasename(name, symlinkHashes[name])))
                  ? symlinkHashes[name]
                  : await hashAndLink(name);
              hashed[name] = { hash };
              if (shouldLog) hashLog(src, hashedBasename(name, hash), pkg?.name);
            }),
          );
          if (update && pkg?.root) {
            const replacements: Record<string, string> = {};
            for (const src of fullList.map(f => relative(env.outDir, f))) {
              replacements[src] = `hashed/${hashedBasename(src, hashed[src].hash!)}`;
            }
            const { name, hash } = await replaceAllWithHashUrls(update, replacements);
            hashed[name] = { hash };
            if (shouldLog) hashLog(name, hashedBasename(name, hash), pkg.name);
          }
          updateManifest({ hashed });
        },
      }),
    ),
  );
}

export async function symlinkTargetHashes(newLinks?: string[]) {
  const targetHashes = {} as Record<string, string>;
  if (newLinks && newLinks.length === 0) return targetHashes;

  await fs.promises.readdir(env.hashOutDir).then(files =>
    Promise.all(
      files.map(async symlink => {
        const [, hash] = symlink.match(/^.+\.([0-9a-f]{8})(?:\.[^\.]*)?$/) ?? [];
        if (!hash || (newLinks && !newLinks.some(l => l.endsWith(symlink)))) return;
        const absSymlink = join(env.hashOutDir, symlink);
        try {
          const [target, stale] = await Promise.all([fs.promises.readlink(absSymlink), isLinkStale(symlink)]);
          if (!stale) targetHashes[relative(env.outDir, resolve(env.hashOutDir, target))] = hash;
        } catch {}
      }),
    ),
  );
  return targetHashes;
}

export function hashedBasename(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.lastIndexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}

async function isLinkStale(symlink: string | undefined) {
  if (!symlink) return true;
  const absSymlink = join(env.hashOutDir, symlink);
  const [{ mtimeMs: linkMs }, { mtimeMs: targetMs }] = await Promise.all([
    fs.promises.lstat(absSymlink),
    fs.promises.stat(absSymlink),
  ]);
  return !isClose(linkMs, targetMs);
}

async function replaceAllWithHashUrls(name: string, files: Record<string, string>) {
  const result = Object.entries(files).reduce(
    (data, [from, to]) => data.replaceAll(from, to),
    await fs.promises.readFile(name, 'utf8'),
  );
  const hash = crypto.createHash('sha256').update(result).digest('hex').slice(0, 8);
  await fs.promises.writeFile(join(env.hashOutDir, hashedBasename(name, hash)), result);
  return { name: relative(env.outDir, name), hash };
}

async function hashAndLink(name: string) {
  const src = join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  const link = join(env.hashOutDir, hashedBasename(name, hash));
  const [{ mtime }] = await Promise.all([
    fs.promises.stat(join(env.outDir, name)),
    fs.promises.symlink(relative(env.outDir, name), link).catch(() => {}),
  ]);
  await fs.promises.lutimes(link, mtime, mtime);
  return hash;
}

function hashLog(src: string, hashName: string, pkgName?: string) {
  env.log(
    `${pkgName ? c.grey(pkgName) + ' ' : ''}'${c.cyan(src)}' -> '${c.cyan(join('public', 'hashed', hashName))}'`,
    'hash',
  );
}
