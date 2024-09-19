import fs from 'node:fs/promises';
import path from 'node:path';
import { Worker } from 'node:worker_threads';
import { env, colors as c, errorMark } from './main';
import { globArray, folderSize } from './parse';
import type { WorkerData, Message } from './tscWorker';
import ts from 'typescript';

const workers: Worker[] = [];

export async function stopTsc(): Promise<void> {
  await Promise.allSettled(workers.map(w => w.terminate()));
  workers.length = 0;
}

export async function tsc(): Promise<void> {
  if (!env.tsc) return;
  await Promise.allSettled([
    fs.mkdir(path.join(env.buildTempDir, 'noCheck')),
    fs.mkdir(path.join(env.buildTempDir, 'noEmit')),
  ]);

  const buildPaths = (await globArray('*/tsconfig*.json', { cwd: env.uiDir }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));

  const configs = (await Promise.all(buildPaths.map(splitConfig))).sort((a, b) => b.size - a.size);

  const noCheckWorkerBuckets: SplitConfig[][] = Array.from({ length: 4 }, () => []);
  const noEmitWorkerBuckets: SplitConfig[][] = Array.from({ length: 8 }, () => []);

  // traverse packages by descending src folder size and assign to emptiest available worker buckets
  for (const config of configs) {
    if (config.noCheck) addToBucket(config, noCheckWorkerBuckets, 'noCheck');
    addToBucket(config, noEmitWorkerBuckets, 'noEmit');
  }

  env.log(`Transpiling ${c.grey('noCheck')}`, { ctx: 'tsc' });
  await new WatchMonitor(noCheckWorkerBuckets, 'noCheck').ok;

  env.log(`Typechecking ${c.grey('noEmit')}`, { ctx: 'tsc' });
  await new WatchMonitor(noEmitWorkerBuckets, 'noEmit').ok;
}

class WatchMonitor {
  status: ('ok' | 'busy' | 'error')[] = [];
  resolve?: () => void;
  ok = new Promise<void>(resolve => (this.resolve = resolve));
  constructor(
    readonly buckets: SplitConfig[][],
    readonly key: 'noCheck' | 'noEmit',
  ) {
    for (const bucket of buckets) {
      const workerData: WorkerData = {
        projects: bucket.map(p => p[key]!.file),
        watch: env.watch,
        index: this.status.length,
      };
      this.status.push('busy');
      const worker = new Worker(path.resolve(__dirname, 'tscWorker.js'), { workerData });
      workers.push(worker);
      worker.on('message', this.onMessage);
      worker.on('error', this.onError);
    }
  }
  onMessage = (msg: Message): void => {
    // the watch builder always gives us a 6194 first time thru, even when errors are found
    if (env.watch && this.resolve && msg.type === 'ok' && this.status[msg.index] === 'error') return;

    this.status[msg.index] = msg.type;

    if (msg.type === 'error') return logMessage(msg);
    if (this.status.some(s => s !== 'ok')) return;

    this.resolve?.();
    if (this.key === 'noEmit' && env.exitCode.get('tsc') !== 0) env.done(0, 'tsc');
  };
  onError = (err: Error): void => {
    env.exit(err.message, 'tsc');
  };
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

function addToBucket(config: SplitConfig, buckets: SplitConfig[][], key: keyof SplitConfig) {
  buckets
    .reduce((smallest, current) => {
      const smallestSize = smallest.reduce((sum, cfg) => sum + (cfg[key] ? cfg.size : 0), 0);
      const currentSize = current.reduce((sum, cfg) => sum + (cfg[key] ? cfg.size : 0), 0);
      return currentSize < smallestSize ? current : smallest;
    })
    .push(config);
}

export function zip<T, U>(arr1: T[], arr2: U[]): [T, U][] {
  const length = Math.min(arr1.length, arr2.length);
  const res: [T, U][] = [];
  for (let i = 0; i < length; i++) {
    res.push([arr1[i], arr2[i]]);
  }
  return res;
}

// the splitConfig transform generates noCheck and noEmit tsconfigs within a ui/.build temp folder.
// each package that emits gets a --noCheck pass for fast transpilations and declarations.
// then we do a --noEmit pass on EVERY package to verify things, regardless of its original emit.
// this allows more parallel type checking.

// splitConfig expects current lichess tsconfig conventions and doesn't anticipate future needs

interface SplitConfig {
  noEmit: { file: string; data: any };
  noCheck?: { file: string; data: any };
  pkgName: string;
  size: number;
}

async function splitConfig(cfgPath: string): Promise<SplitConfig> {
  const root = path.dirname(cfgPath);
  const pkgName = path.basename(root);
  const { config, error } = ts.readConfigFile(cfgPath, ts.sys.readFile);

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
  config.exclude = config.exclude?.map((glob: string) =>
    path.resolve(root, glob.replace('${configDir}', '.')),
  );
  config.extends = undefined;
  config.references = env.deps
    .get(pkgName)
    ?.map(ref => ({ path: path.join(env.buildTempDir, 'noCheck', `${ref}.tsconfig.json`) }));

  const noEmit = {
    data: structuredClone(config),
    file: path.join(env.buildTempDir, 'noEmit', `${pkgName}.tsconfig.json`),
  };
  noEmit.data.compilerOptions.noEmit = true;
  noEmit.data.compilerOptions.tsBuildInfoFile = path.join(
    env.buildTempDir,
    'noEmit',
    `${pkgName}.tsbuildinfo`,
  );
  await fs.writeFile(noEmit.file, JSON.stringify(noEmit.data, undefined, 2));

  const res: SplitConfig = { noEmit, pkgName, size: await folderSize(path.join(root, 'src')) };

  if (!co.noEmit) {
    res.noCheck = {
      data: structuredClone(config),
      file: path.join(env.buildTempDir, 'noCheck', `${pkgName}.tsconfig.json`),
    };
    res.noCheck.data.compilerOptions.noCheck = true;
    res.noCheck.data.compilerOptions.tsBuildInfoFile = path.join(
      env.buildTempDir,
      'noCheck',
      `${pkgName}.tsbuildinfo`,
    );
    await fs.writeFile(res.noCheck.file, JSON.stringify(res.noCheck.data, undefined, 2));
  }
  return res;
}
