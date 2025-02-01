import fs from 'node:fs';
import p from 'node:path';
import { task } from './task.ts';
import { env, c } from './env.ts';
import { isGlob, isFolder } from './parse.ts';
import { quantize, isEquivalent } from './algo.ts';

export async function sync(): Promise<any> {
  if (!env.begin('sync')) return;
  return Promise.all(
    [...env.tasks('sync')].map(async ([pkg, sync]) => {
      const root = await syncRoot(env.rootDir, sync.src);
      await task({
        glob: { path: sync.src, cwd: env.rootDir },
        ctx: 'sync',
        debounce: 300,
        execute: (files, fullList) => {
          const logEvery = !isEquivalent(files, fullList);
          if (!logEvery)
            env.log(`${c.grey(pkg.name)} '${c.cyan(sync.src)}' -> '${c.cyan(sync.dest)}'`, 'sync');
          return Promise.all(
            files.map(async f => {
              if ((await syncOne(f, p.join(env.rootDir, sync.dest, f.slice(root.length)))) && logEvery)
                env.log(
                  `${c.grey(pkg.name)} '${c.cyan(f.slice(root.length))}' -> '${c.cyan(sync.dest)}'`,
                  'sync',
                );
            }),
          );
        },
      });
    }),
  );
}

async function syncOne(absSrc: string, absDest: string): Promise<boolean> {
  const [src, dest] = (
    await Promise.allSettled([
      fs.promises.stat(absSrc),
      fs.promises.stat(absDest),
      fs.promises.mkdir(p.dirname(absDest), { recursive: true }),
    ])
  ).map(x => (x.status === 'fulfilled' ? (x.value as fs.Stats) : undefined));
  if (src && (!dest || quantize(src.mtimeMs, 300) !== quantize(dest.mtimeMs, 300))) {
    await fs.promises.copyFile(absSrc, absDest);
    await fs.promises.utimes(absDest, src.atime, src.mtime);
    return true;
  }
  return false;
}

async function syncRoot(cwd: string, path: string): Promise<string> {
  if (!(isGlob(path) || (await isFolder(p.join(cwd, path))))) return p.join(cwd, p.dirname(path));
  const [head, ...tail] = path.split(p.sep);
  if (isGlob(head)) return cwd;
  return syncRoot(p.join(cwd, head), tail.join(p.sep));
}
