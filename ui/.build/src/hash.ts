import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { globArray } from './parse.ts';
import { updateManifest } from './manifest.ts';
import { env } from './env.ts';

export async function hash(): Promise<void> {
  const newHashLinks = new Map<string, number>();
  const alreadyHashed = new Map<string, string>();
  const hashed = (
    await Promise.all(
      env.building.flatMap(pkg =>
        pkg.hash.map(async hash =>
          (await globArray(hash.glob, { cwd: env.outDir })).map(path => ({
            path,
            update: hash.update,
            root: pkg.root,
          })),
        ),
      ),
    )
  ).flat();

  const sourceStats = await Promise.all(hashed.map(hash => fs.promises.stat(hash.path)));

  for (const [i, stat] of sourceStats.entries()) {
    const name = hashed[i].path.slice(env.outDir.length + 1);
    if (stat.mtimeMs === env.manifest.hashed[name]?.mtime)
      alreadyHashed.set(name, env.manifest.hashed[name].hash!);
    else newHashLinks.set(name, stat.mtimeMs);
  }
  await Promise.allSettled([...alreadyHashed].map(([name, hash]) => link(name, hash)));

  for await (const { name, hash } of [...newHashLinks.keys()].map(hashLink)) {
    env.manifest.hashed[name] = Object.defineProperty({ hash }, 'mtime', { value: newHashLinks.get(name) });
  }
  if (newHashLinks.size === 0 && alreadyHashed.size === Object.keys(env.manifest.hashed).length) return;

  for (const key of Object.keys(env.manifest.hashed)) {
    if (!hashed.some(x => x.path.endsWith(key))) delete env.manifest.hashed[key];
  }

  const updates: Map<string, { root: string; mapping: Record<string, string> }> = new Map();
  for (const { root, path, update } of hashed) {
    if (!update) continue;
    const updateFile = updates.get(update) ?? { root, mapping: {} };
    const from = path.slice(env.outDir.length + 1);
    updateFile.mapping[from] = asHashed(from, env.manifest.hashed[from].hash!);
    updates.set(update, updateFile);
  }
  for await (const { name, hash } of [...updates].map(([n, r]) => update(n, r.root, r.mapping))) {
    env.manifest.hashed[name] = { hash };
  }
  updateManifest({ dirty: true });
}

async function update(name: string, root: string, files: Record<string, string>) {
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
