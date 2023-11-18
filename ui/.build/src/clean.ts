import { promises as fs } from 'fs';
import * as fg from 'fast-glob';
import { env, colors as c } from './main';

const globs = [
  '**/node_modules',
  'ui/*/dist',
  'ui/*/tsconfig.tsbuildinfo',
  'public/compiled',
  'public/npm',
  'public/css/*.css*',
];

const globOpts: fg.Options = {
  absolute: true,
  onlyFiles: false,
  followSymbolicLinks: false,
  markDirectories: true,
};

export async function clean() {
  if (!env.clean) return;
  for (const glob of globs) {
    env.log(`Cleaning '${c.cyan(glob)}'...`);
    for await (const f of fg.stream(glob, { cwd: env.rootDir, ...globOpts })) {
      if (f.includes('ui/.build')) continue;
      if (f[f.length - 1] === '/') await fs.rm(f, { recursive: true });
      else await fs.unlink(f);
    }
  }
}
