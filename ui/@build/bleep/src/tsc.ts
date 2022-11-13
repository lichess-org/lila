import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { modules, moduleDeps } from './build';
import { LichessModule, env, colors as c, errorMark, lines } from './main';

export async function makeBleepConfig(buildModules: LichessModule[]): Promise<void> {
  const tsc: string[] = [];
  await fs.promises.rm(env.tsconfigDir, { recursive: true, force: true });
  await fs.promises.mkdir(env.tsconfigDir);

  buildModules.forEach(mod => {
    moduleDeps.get(mod.name)?.forEach(dep => {
      const depMod = modules.get(dep);
      if (depMod?.tscOptions && !depMod.tscOptions?.includes('composite')) {
        depMod.tscOptions!.push('composite');
      }
    });
  });

  for (const mod of buildModules) {
    const tsconfig = await makeTsConfig(mod, env.tsc || !!mod.tscOptions, !mod.tscOptions);
    if (env.tsc || mod.tscOptions) tsc.push(tsconfig);
  }
  const cfg: any = {};
  cfg.files = [];
  cfg.references = tsc.map(p => ({ path: p }));
  await fs.promises.writeFile(path.join(env.tsconfigDir, 'bleep.tsconfig.json'), JSON.stringify(cfg));
}

export function typescriptWatch(success: () => void) {
  let watching = false;
  const tsc = cps.spawn(
    'tsc',
    ['-b', path.join(env.tsconfigDir, 'bleep.tsconfig.json'), '--incremental', '-w', '--preserveWatchOutput'],
    { cwd: env.tsconfigDir }
  );
  tsc.stdout?.on('data', (buf: Buffer) => {
    // no way to magically get build events...
    // look at in-process typescript invocation because interpreting stdout is dirty.
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) {
      if (txt.includes('Found 0 errors')) {
        env.good('tsc');
        if (!watching) {
          success();
          watching = true;
        }
      } else if (txt.includes('File change detected') || txt.includes('Starting compilation')) {
        env.log(`Checking typescript`, { ctx: 'tsc' });
      } else {
        terseTsc(txt);
      }
    }
  });
  tsc.stderr?.on('data', txt => env.log(txt, { ctx: 'tsc', error: true }));
}

async function makeTsConfig(mod: LichessModule, refs: boolean, noEmit: boolean): Promise<string> {
  const resolveThese = ['include', 'exclude', 'outDir', 'typeRoots', 'src', 'baseUrl', 'extends', 'path'];

  const resolvePaths = (o: any, forceAll = false): any => {
    for (const key in o) {
      if (forceAll || resolveThese.includes(key)) {
        if (typeof o[key] === 'string') o[key] = path.join(mod.root, o[key]);
        else if (Array.isArray(o[key])) o[key] = o[key].map((p: string) => path.join(mod.root, p));
      } else if (typeof o[key] === 'object' && !Array.isArray(o[key])) {
        o[key] = resolvePaths(o[key], key === 'paths'); // resolve all values in 'paths' element
      }
    }
    return o;
  };
  const srcConfig = path.join(mod.root, 'tsconfig.json');
  const cfg = fs.existsSync(srcConfig) ? resolvePaths(JSON.parse(await fs.promises.readFile(srcConfig, 'utf8'))) : {};

  if (!('include' in cfg)) {
    cfg.include = [path.join(env.uiDir, mod.name, 'src')];
  }
  if (!('compilerOptions' in cfg)) {
    cfg.compilerOptions = {};
  }
  if (noEmit && cfg.compilerOptions.noEmit !== false) {
    cfg.compilerOptions.noEmit = true;
  } else if (!('outDir' in cfg.compilerOptions)) {
    cfg.compilerOptions.outDir = path.join(env.uiDir, mod.name, 'dist');
  }
  if (!('rootDir' in cfg.compilerOptions)) {
    cfg.compilerOptions.rootDir = path.join(env.uiDir, mod.name, 'src');
  }
  mod.tscOptions?.forEach(option => (cfg.compilerOptions[option] = true));

  const deps = moduleDeps.get(mod.name);
  if (refs && deps) cfg['references'] = deps.map(dep => ({ path: `${dep}.tsconfig.json` }));

  const configName = `${mod.name}.tsconfig.json`;
  await fs.promises.writeFile(path.join(env.tsconfigDir, configName), JSON.stringify(cfg));
  return configName;
}

function terseTsc(text: string): void {
  text = text.replace(/\d?\d:\d\d:\d\d (PM|AM) - /, '');
  if (text.match(/: error TS\d\d\d\d/)) {
    // strip the ../../../../.. junk, highlight error
    text = `${errorMark} - ${text.replace(/^[./]*/, '')}`;
  }
  env.log(text.replace('. Watching for file changes.', ` - ${c.grey('Watching')}...`), { ctx: 'tsc' });
}
