import * as fs from 'node:fs';
import * as path from 'node:path';
import * as ps from 'node:process';
import { buildModules, build } from './build';
import { env } from './main';

const allSettledMs = 2000; // must elapse after tickle to trigger rebuild
const pkgWatch: fs.FSWatcher[] = [];
let rebuildTimeout: NodeJS.Timeout | undefined;

export function killRebuildWatch() {
  for (const pkg of pkgWatch) pkg.close();
  pkgWatch.length = 0;
  clearTimeout(rebuildTimeout);
  rebuildTimeout = undefined;
}

// we could rebuild only modules with dep changes and their deps one day but this works for now
export function rebuildWatch(mods: string[]) {
  if (!env.watch) return;

  const packageChange = () => {
    if (env.rebuild) {
      clearTimeout(rebuildTimeout);
      rebuildTimeout = setTimeout(() => build(mods), allSettledMs);
      return;
    }
    env.warn('Exiting due to package.json change');
    env.warn('Use --rebuild or -r to reinstall & rebuild rather than exit');
    ps.exit(0);
  };

  pkgWatch.push(fs.watch(path.join(env.rootDir, 'package.json'), packageChange));
  for (const mod of buildModules) {
    pkgWatch.push(fs.watch(path.join(mod.root, 'package.json'), packageChange));
    pkgWatch.push(fs.watch(path.join(mod.root, 'tsconfig.json'), packageChange));
  }
}
