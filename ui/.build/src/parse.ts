import * as fs from 'node:fs';
import * as path from 'node:path';
import * as fg from 'fast-glob';
import { LichessModule, env, colors as c } from './main';

export const parseModules = async (): Promise<[Map<string, LichessModule>, Map<string, string[]>]> => {
  const modules = new Map<string, LichessModule>();
  const moduleDeps = new Map<string, string[]>();

  for (const dir of (await globArray('[^@.]*/package.json')).map(pkg => path.dirname(pkg))) {
    const mod = await parseModule(dir);
    modules.set(mod.name, mod);
  }

  for (const mod of modules.values()) {
    const deplist: string[] = [];
    for (const dep in mod.pkg.dependencies) {
      if (modules.has(dep)) deplist.push(dep);
    }
    moduleDeps.set(mod.name, deplist);
  }
  return [modules, moduleDeps];
};

export async function globArray(
  glob: string,
  { cwd = env.uiDir, abs = true, dirs = false } = {},
): Promise<string[]> {
  const files: string[] = [];
  for await (const f of fg.stream(glob, { cwd, absolute: abs, onlyFiles: !dirs }))
    files.push(f.toString('utf8'));
  return files;
}

async function parseModule(moduleDir: string): Promise<LichessModule> {
  const pkg = JSON.parse(await fs.promises.readFile(path.join(moduleDir, 'package.json'), 'utf8'));
  const mod: LichessModule = {
    pkg: pkg,
    name: path.basename(moduleDir),
    root: moduleDir,
    pre: [],
    post: [],
    hasTsconfig: fs.existsSync(path.join(moduleDir, 'tsconfig.json')),
  };

  if ('lichess' in pkg && 'bundles' in pkg.lichess) {
    if (typeof pkg.lichess.bundles === 'string') mod.bundles = [pkg.lichess.bundles];
    else mod.bundles = pkg.lichess.bundles as string[];
  }
  if ('lichess' in pkg && 'sync' in pkg.lichess) {
    mod.sync = Object.entries(pkg.lichess.sync).map(x => ({
      src: x[0],
      dest: x[1] as string,
      mod,
    }));
  }
  return mod;
}
