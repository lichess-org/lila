import { promises as fs } from 'node:fs';
import fg from 'fast-glob';
import { env, c } from './env.ts';

const globOpts: fg.Options = {
  absolute: true,
  onlyFiles: false,
  followSymbolicLinks: false,
  markDirectories: true,
};

const allGlobs = [
  '**/node_modules',
  '**/css/**/gen',
  'ui/.build/build',
  'ui/*/dist',
  'ui/*/tsconfig.tsbuildinfo',
  'public/compiled',
  'public/npm',
  'public/css',
  'public/hashed',
];

export async function clean(globs?: string[] | 'force'): Promise<void> {
  if (!env.clean && !globs) return;

  for (const glob of Array.isArray(globs) ? globs : allGlobs) {
    env.log(`Cleaning '${c.cyan(glob)}'...`);
    for await (const f of fg.stream(glob, { cwd: env.rootDir, ...globOpts })) {
      if (f.includes('ui/.build') && !f.includes('/build')) continue; // skip .build/node_modules
      if (f.slice(-1) === '/') await fs.rm(f, { recursive: true });
      else await fs.unlink(f);
    }
  }
}

export async function deepClean(): Promise<void> {
  return clean(['ui/@types/lichess/i18n.d.ts', 'translation/js', ...allGlobs]);
}
