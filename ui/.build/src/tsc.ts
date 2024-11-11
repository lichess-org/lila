import fs from 'node:fs';
import path from 'node:path';
import { Worker } from 'node:worker_threads';
import { env, colors as c, errorMark } from './main.ts';
import { globArray, folderSize } from './parse.ts';
import type { WorkerData, Message } from './tscWorker.ts';
import ts from 'typescript';

const workers: Worker[] = [];

export async function stopTsc(): Promise<void> {
  await Promise.allSettled(workers.map(w => w.terminate()));
  workers.length = 0;
}

export async function tsc(): Promise<void> {
  if (!env.tsc) return;
  await Promise.allSettled([
    fs.promises.mkdir(path.join(env.buildTempDir, 'noCheck')),
    fs.promises.mkdir(path.join(env.buildTempDir, 'noEmit')),
  ]);

  const buildPaths = (await globArray('*/tsconfig*.json', { cwd: env.uiDir }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));

  const workerBuckets = {
    noCheck: Array.from({ length: 4 }, () => []),
    noEmit: Array.from({ length: 8 }, () => []),
  };

  // traverse packages by descending src folder size and assign to emptiest available worker buckets
  for (const cfg of (await Promise.all(buildPaths.map(splitConfig))).flat().sort((a, b) => b.size - a.size)) {
    addToBucket(cfg, workerBuckets[cfg.type]);
  }

  env.log(`Transpiling ${c.grey('noCheck')}`, { ctx: 'tsc' });
  await watchMonitor(workerBuckets.noCheck, 'noCheck');

  env.log(`Typechecking ${c.grey('noEmit')}`, { ctx: 'tsc' });
  await watchMonitor(workerBuckets.noEmit, 'noEmit');
}

function watchMonitor(buckets: SplitConfig[][], key: 'noCheck' | 'noEmit') {
  const status: ('ok' | 'busy' | 'error')[] = [];
  let resolve: (() => void) | undefined = undefined;
  const ok = new Promise<void>(res => (resolve = res));

  const onMessage = (msg: Message): void => {
    // the watch builder always gives us a 6194 first time thru, even when errors are found
    if (env.watch && resolve && msg.type === 'ok' && status[msg.index] === 'error') return;

    status[msg.index] = msg.type;

    if (msg.type === 'error') return logMessage(msg);
    if (status.some(s => s !== 'ok')) return;

    resolve?.();
    if (key === 'noEmit' && env.exitCode.get('tsc') !== 0) env.done(0, 'tsc');
  };
  const onError = (err: Error): void => {
    env.exit(err.message, 'tsc');
  };

  for (const bucket of buckets) {
    const workerData: WorkerData = {
      projects: bucket.map(p => p.configFile),
      watch: env.watch,
      index: status.length,
    };
    status.push('busy');
    const worker = new Worker(path.resolve(env.buildSrcDir, 'tscWorker.ts'), { workerData });
    workers.push(worker);
    worker.on('message', onMessage);
    worker.on('error', onError);
  }
  return ok;
}

function logMessage(msg: Message): void {
  const { code, text, file, line, col } = msg.data;
  const prelude = `${errorMark} ts${code} `;
  const message = `${ts.flattenDiagnosticMessageText(text, '\n', 0)}`;
  let location = '';
  if (file) {
    location = `${c.grey('in')} '${c.cyan(path.relative(env.uiDir, file))}`;
    if (line !== undefined) location += c.grey(`:${line + 1}:${col + 1}`);
    location += `' - `;
  }
  env.log(`${prelude}${location}${message}`, { ctx: 'tsc' });
  if (!env.exitCode.get('tsc')) env.done(1, 'tsc');
}

function addToBucket(config: SplitConfig, buckets: SplitConfig[][]) {
  buckets
    .reduce((smallest, current) => {
      const smallestSize = smallest.reduce((sum, cfg) => sum + cfg.size, 0);
      const currentSize = current.reduce((sum, cfg) => sum + cfg.size, 0);
      return currentSize < smallestSize ? current : smallest;
    })
    .push(config);
}

// the splitConfig transform generates noCheck and noEmit tsconfigs within the 'build' temp folder.
// each package that emits gets a --noCheck pass for fast transpilations and declarations.
// then we do a --noEmit pass on EVERY package to verify things, regardless of its original emit.
// this allows more parallel type checking.

interface SplitConfig {
  type: 'noEmit' | 'noCheck';
  configFile: string;
  pkgName: string;
  size: number;
}

async function splitConfig(cfgPath: string): Promise<SplitConfig[]> {
  const root = path.dirname(cfgPath);
  const pkgName = path.basename(root);
  const { config, error } = ts.readConfigFile(cfgPath, ts.sys.readFile);
  const io: Promise<any>[] = [];

  if (error) throw new Error(`Error reading tsconfig.json: ${error.messageText}`);

  const co: any = ts.parseJsonConfigFileContent(config, ts.sys, path.dirname(cfgPath)).options;

  if (co.moduleResolution) co.moduleResolution = ts.ModuleResolutionKind[co.moduleResolution];
  if (co.module) co.module = ts.ModuleKind[co.module];
  if (co.target) co.target = ts.ScriptTarget[co.target];
  co.lib?.forEach((lib: string, i: number) => {
    if (lib.startsWith('lib.')) co.lib[i] = lib.split('.')[1];
  });
  co.incremental = true;

  config.compilerOptions = co;
  config.size = await folderSize(path.join(root, 'src'));
  config.include = config.include?.map((glob: string) =>
    path.resolve(root, glob.replace('${configDir}', '.')),
  );
  config.include ??= [co.rootDir ? `${co.rootDir}/**/*` : `${root}/src/**/*`];
  config.exclude = config.exclude
    ?.filter((glob: string) => !env.test || !glob.includes('tests'))
    .map((glob: string) => path.resolve(root, glob.replace('${configDir}', '.')));
  config.extends = undefined;
  config.references = env.deps
    .get(pkgName)
    ?.map(ref => ({ path: path.join(env.buildTempDir, 'noCheck', `${ref}.tsconfig.json`) }));

  const noEmitData = structuredClone(config);
  const noEmit = path.join(env.buildTempDir, 'noEmit', `${pkgName}.tsconfig.json`);
  noEmitData.compilerOptions.noEmit = true;
  noEmitData.compilerOptions.tsBuildInfoFile = path.join(
    env.buildTempDir,
    'noEmit',
    `${pkgName}.tsbuildinfo`,
  );
  if (env.test && fs.existsSync(path.join(root, 'tests'))) {
    noEmitData.include.push(path.join(root, 'tests'));
    noEmitData.compilerOptions.rootDir = root;
    noEmitData.compilerOptions.skipLibCheck = true;
    noEmitData.size += await folderSize(path.join(root, 'tests'));
  }
  io.push(fs.promises.writeFile(noEmit, JSON.stringify(noEmitData, undefined, 2)));

  const res: SplitConfig[] = [{ type: 'noEmit', configFile: noEmit, pkgName, size: config.size }];

  if (!co.noEmit) {
    const noCheckData = structuredClone(config);
    const noCheck = path.join(env.buildTempDir, 'noCheck', `${pkgName}.tsconfig.json`);
    noCheckData.compilerOptions.noCheck = true;
    noCheckData.compilerOptions.tsBuildInfoFile = path.join(
      env.buildTempDir,
      'noCheck',
      `${pkgName}.tsbuildinfo`,
    );
    res.push({ type: 'noCheck', configFile: noCheck, pkgName, size: config.size });
    io.push(fs.promises.writeFile(noCheck, JSON.stringify(noCheckData, undefined, 2)));
  }
  await Promise.all(io);
  return res;
}
