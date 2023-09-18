import * as fs from 'node:fs';
import * as path from 'node:path';
import * as fg from 'fast-glob';
import { LichessModule, env, colors as c } from './main';

export const parseModules = async (): Promise<[Map<string, LichessModule>, Map<string, string[]>]> => {
  const modules = new Map<string, LichessModule>();
  const moduleDeps = new Map<string, string[]>();

  for (const dir of (await globArray('[^@]*/package.json')).map(pkg => path.dirname(pkg))) {
    const mod = await parseModule(dir);
    modules.set(mod.name, mod);
  }

  for (const mod of modules.values()) {
    const deplist: string[] = [];
    for (const dep in mod.pkg.dependencies) {
      if (modules.has(dep)) deplist.push(dep);
    }
    moduleDeps.set(mod.name, deplist);
    // for package.jsons with multiple esm bundles, subsequent bundles depend on the first
    mod.bundles?.esm?.slice(1).forEach(r => moduleDeps.set(r.output, [mod.name, ...deplist]));
  }
  return [modules, moduleDeps];
};

export async function globArray(glob: string, { cwd = env.uiDir, abs = true } = {}): Promise<string[]> {
  const files: string[] = [];
  for await (const file of fg.stream(glob, { cwd: cwd, absolute: abs })) files.push(file.toString('utf8'));
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
  parseScripts(mod, 'scripts' in pkg ? pkg.scripts : {});

  if ('lichess' in pkg && 'modules' in pkg.lichess) {
    for (const moduleType in pkg.lichess.modules) {
      if (moduleType !== 'esm' && moduleType !== 'iife') {
        env.log(
          c.warn('WARNING') +
            ` - Unsupported module type '${c.cyan(moduleType)}' in '${c.cyan(mod.name + '/package.json')}'`,
        );
        continue;
      }
      mod.bundles ??= {};
      mod.bundles[moduleType] = Object.entries(pkg.lichess.modules[moduleType]).map(x => ({
        input: x[0],
        output: x[1] as string,
      }));
    }
  }
  if ('lichess' in pkg && 'copy' in pkg.lichess) {
    const copy: any[] = Array.isArray(pkg.lichess.copy) ? pkg.lichess.copy : [pkg.lichess.copy];
    const flattener = new Map<string, Set<string>>();
    for (const s of copy) {
      if (!Array.isArray(s.src)) s.src = [s.src];
      for (const src of s.src) {
        const srcDest = flattener.get(src) ?? new Set<string>();
        srcDest.add(s.dest);
        flattener.set(src, srcDest);
      }
    }
    mod.copy = [];
    for (const [src, dests] of flattener.entries())
      for (const dest of dests) mod.copy.push({ src, dest, mod });
  }
  return mod;
}

function tokenizeArgs(argstr: string): string[] {
  const args: string[] = [];
  const reducer = (a: any[], ch: string) => {
    if (ch !== ' ') return ch === "'" ? [a[0], !a[1]] : [a[0] + ch, a[1]];
    if (a[1]) return [a[0] + ' ', true];
    else if (a[0]) args.push(a[0]);
    return ['', false];
  };
  const lastOne = [...argstr].reduce(reducer, ['', false])[0];
  return lastOne ? [...args, lastOne] : args;
}

// go through package json scripts and get what we need from 'compile', 'dev', and 'deps'
// if some other script is necessary, add it to buildScriptKeys
function parseScripts(module: LichessModule, pkgScripts: any) {
  const buildScriptKeys = ['deps', 'compile', 'dev', 'post'].concat(env.prod ? ['prod'] : []);

  for (const script in pkgScripts) {
    if (!buildScriptKeys.includes(script)) continue;
    pkgScripts[script].split(/&&/).forEach((cmd: string) => {
      // no need to support || in a script property yet, we don't even short circuit && properly
      const args = tokenizeArgs(cmd.trim());
      if (!['$npm_execpath', 'tsc'].includes(args[0])) {
        script == 'prod' || script == 'post' ? module.post.push(args) : module.pre.push(args);
      }
    });
  }
}
