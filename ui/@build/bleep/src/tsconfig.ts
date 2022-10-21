import * as fs from 'node:fs';
import * as path from 'node:path';
import { modules, moduleDeps } from './build';
import { LichessModule, env } from './env';

export async function makeBleepConfig(buildModules: LichessModule[]): Promise<void> {
  const tsc: string[] = [];
  await fs.promises.rm(env.tsconfigDir, { recursive: true, force: true });
  await fs.promises.mkdir(env.tsconfigDir);

  buildModules.forEach(mod => {
    moduleDeps.get(mod.name)?.forEach(dep => {
      const depMod = modules.get(dep);
      if (depMod?.tscOptions && !depMod.tscOptions?.includes('composite')) depMod.tscOptions!.push('composite');
    });
  });

  for (const mod of buildModules) {
    if (mod.tscOptions) tsc.push(await makeTsConfig(mod, true));
    else await makeTsConfig(mod, false);
  }
  const cfg: any = {};
  cfg.files = [];
  cfg.references = tsc.map(p => ({ path: p }));
  await fs.promises.writeFile(path.resolve(env.tsconfigDir, 'bleep.tsconfig.json'), JSON.stringify(cfg));
}

async function makeTsConfig(mod: LichessModule, withRefs: boolean): Promise<string> {
  const resolveThese = ['include', 'exclude', 'outDir', 'src', 'baseUrl', 'extends', 'path'];

  const absolutePaths = (o: any, forceAll = false): any => {
    for (const key in o) {
      if (forceAll || resolveThese.includes(key)) {
        if (typeof o[key] == 'string') o[key] = path.resolve(mod.root, o[key]);
        else if (Array.isArray(o[key])) o[key] = o[key].map((p: string) => path.resolve(mod.root, p));
      } else if (typeof o[key] === 'object' && !Array.isArray(o[key])) {
        o[key] = absolutePaths(o[key], key == 'paths'); // resolve all values in 'paths' element
      }
    }
    return o;
  };
  const srcConfig = path.resolve(mod.root, 'tsconfig.json');
  const config = fs.existsSync(srcConfig)
    ? absolutePaths(JSON.parse(await fs.promises.readFile(srcConfig, 'utf8')))
    : {}; // i love you tutor

  if (!('include' in config)) config.include = [path.resolve(env.uiDir, mod.name, 'src')];
  if (!('compilerOptions' in config)) config.compilerOptions = {};

  config.compilerOptions.rootDir = path.resolve(env.uiDir, mod.name, 'src');
  //config.compilerOptions.baseUrl = path.resolve(env.uiDir, mod.name );
  mod.tscOptions?.forEach(option => (config.compilerOptions[option] = true));

  const deps = moduleDeps.get(mod.name);
  if (withRefs && deps) config['references'] = deps.map(dep => ({ path: `${dep}.tsconfig.json` }));

  const configName = `${mod.name}.tsconfig.json`;
  await fs.promises.writeFile(path.resolve(env.tsconfigDir, configName), JSON.stringify(config));
  return configName;
}
