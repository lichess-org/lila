import fs from 'node:fs';
import p from 'node:path';
import crypto from 'node:crypto';
import { task } from './task.ts';
import { type Manifest, updateManifest } from './manifest.ts';
import { env, c } from './env.ts';
import type { Package } from './parse.ts';
import { isEquivalent } from './algo.ts';

export async function hash(): Promise<void> {
  if (!env.begin('hash')) return;
  const hashed: Manifest = {};
  const pathOnly: { glob: string[] } = { glob: [] };
  const hashRuns: { glob: string | string[]; update?: string; pkg?: Package }[] = [];

  for (const [pkg, { glob, update, ...rest }] of env.tasks('hash')) {
    update ? hashRuns.push({ glob, update, pkg, ...rest }) : pathOnly.glob.push(glob);
    env.log(`${c.grey(pkg.name)} '${c.cyan(glob)}' -> '${c.cyan('public/hashed')}'`, 'hash');
  }
  if (pathOnly.glob.length) hashRuns.push(pathOnly);

  await fs.promises.mkdir(env.hashOutDir).catch(() => {});
  await Promise.all(
    hashRuns.map(({ glob, update, pkg }) =>
      task({
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
              const { name, hash } = await hashLink(p.relative(env.outDir, src));
              hashed[name] = { hash };
              if (shouldLog)
                env.log(
                  `'${c.cyan(src)}' -> '${c.cyan(p.join('public', 'hashed', asHashed(name, hash)))}'`,
                  'hash',
                );
            }),
          );
          if (update && pkg?.root) {
            const updates: Record<string, string> = {};
            for (const src of fullList.map(f => p.relative(env.outDir, f))) {
              updates[src] = asHashed(src, hashed[src].hash!);
            }
            const { name, hash } = await replaceHash(p.relative(pkg.root, update), pkg.root, updates);
            hashed[name] = { hash };
            if (shouldLog)
              env.log(
                `${c.grey(pkg.name)} '${c.cyan(name)}' -> '${c.cyan(p.join('public', 'hashed', asHashed(name, hash)))}'`,
                'hash',
              );
          }
          updateManifest({ hashed, merge: true });
        },
      }),
    ),
  );
}

async function replaceHash(name: string, root: string, files: Record<string, string>) {
  const result = Object.entries(files).reduce(
    (data, [from, to]) => data.replaceAll(from, to),
    await fs.promises.readFile(p.join(root, name), 'utf8'),
  );
  const hash = crypto.createHash('sha256').update(result).digest('hex').slice(0, 8);
  await fs.promises.writeFile(p.join(env.hashOutDir, asHashed(name, hash)), result);
  return { name, hash };
}

async function hashLink(name: string) {
  const src = p.join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  await link(name, hash);
  return { name, hash };
}

async function link(name: string, hash: string) {
  const link = p.join(env.hashOutDir, asHashed(name, hash));
  return fs.promises.symlink(p.join('..', name), link).catch(() => {});
}

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.indexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}
