import { promises as fs } from 'fs';
import * as fg from 'fast-glob';
import { env, colors as c } from './main';

const globOpts: fg.Options = {
  absolute: true,
  onlyFiles: false,
  followSymbolicLinks: false,
  markDirectories: true,
};

const globs = [
  '**/node_modules',
  '**/css/**/gen',
  'ui/.build/dist/css',
  'ui/*/dist',
  'ui/*/tsconfig.tsbuildinfo',
  'public/compiled',
  'public/npm',
  'public/css',
];

export async function clean() {
  if (!env.clean) return;

  for (const glob of globs) {
    env.log(`Cleaning '${c.cyan(glob)}'...`);
    for await (const f of fg.stream(glob, { cwd: env.rootDir, ...globOpts })) {
      if (f.includes('ui/.build') && !f.includes('dist/css')) continue;
      if (f[f.length - 1] === '/') await fs.rm(f, { recursive: true });
      else await fs.unlink(f);
    }
  }
}
