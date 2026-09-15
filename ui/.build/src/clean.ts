import fg from 'fast-glob';
import { promises as fs } from 'node:fs';
import pc from 'picocolors';

import { env } from './env.ts';

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

  const startedAt = Date.now();
  const patterns = Array.isArray(globs) ? globs : allGlobs;
  for (const glob of patterns) {
    env.log(`Cleaning '${pc.cyan(glob)}'...`);
  }

  const files = new Set(
    (await Promise.all(patterns.map(glob => fg(glob, { cwd: env.rootDir, ...globOpts }))))
      .flat()
      .filter(f => !f.includes('ui/.build') || f.includes('/build')),
  );
  await Promise.all([...files].map(f => (f.endsWith('/') ? fs.rm(f, { recursive: true }) : fs.unlink(f))));
  env.log(`Done ${pc.gray(`(${((Date.now() - startedAt) / 1000).toFixed(3)}s)`)}`);
}

export async function deepClean(): Promise<void> {
  return clean(['ui/@types/lichess/i18n.d.ts', 'translation/js', ...allGlobs]);
}
