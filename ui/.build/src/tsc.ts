import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import ts from 'typescript';
import { Worker } from 'node:worker_threads';
import { env, colors as c, errorMark } from './env.ts';
import { globArray, folderSize, readable } from './parse.ts';
import { clamp } from './algo.ts';
import type { WorkerData, Message } from './tscWorker.ts';

const workers: Worker[] = [];

export async function tsc(): Promise<void> {
  if (!env.tsc) return;
  await Promise.allSettled([
    fs.promises.mkdir(path.join(env.buildTempDir, 'noCheck')),
    fs.promises.mkdir(path.join(env.buildTempDir, 'noEmit')),
  ]);

  const buildPaths = (await globArray('*/tsconfig*.json', { cwd: env.uiDir }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));

  const logicalCores = os.cpus().length;
  const workBuckets: { [T in 'noCheck' | 'noEmit']: SplitConfig[][] } = {
    noCheck: Array.from({ length: clamp(logicalCores / 4, { min: 1, max: 4 }) }, () => []),
    noEmit: Array.from({ length: clamp(logicalCores / 2, { min: 1, max: 8 }) }, () => []),
  };

  // traverse packages by descending source folder size and assign to emptiest available worker buckets
  for (const cfg of (await Promise.all(buildPaths.map(splitConfig))).flat().sort((a, b) => b.size - a.size))
    workBuckets[cfg.type]
      .reduce((bucket, otherBucket) =>
        bucket.reduce((sz, c) => sz + c.size, 0) < otherBucket.reduce((sz, c) => sz + c.size, 0)
          ? bucket
          : otherBucket,
      )
      .push(cfg);

  tscLog(`Typing ${c.grey('noCheck')} (${workBuckets.noCheck.length} workers)`);
  await assignWork(workBuckets.noCheck, 'noCheck');

  tscLog(`Typechecking ${c.grey('noEmit')} (${workBuckets.noEmit.length} workers)`);
  await assignWork(workBuckets.noEmit, 'noEmit');
}

export async function stopTscWatch(): Promise<void> {
  await Promise.allSettled(workers.map(w => w.terminate()));
  workers.length = 0;
}

function assignWork(buckets: SplitConfig[][], key: 'noCheck' | 'noEmit') {
  let resolve: (() => void) | undefined = undefined;
  const status: ('ok' | 'busy' | 'error')[] = [];
  const ok = new Promise<void>(res => (resolve = res));
  const onError = (err: Error): void => env.exit(err.message, 'tsc');
  const onMessage = (msg: Message): void => {
    // the watch builder always gives us a 6194 first time thru, even when errors are found
    if (env.watch && resolve && msg.type === 'ok' && status[msg.index] === 'error') return;

    status[msg.index] = msg.type;

    if (msg.type === 'error') return tscError(msg);
    if (status.some(s => s !== 'ok')) return;

    resolve?.();
    if (key === 'noEmit' && env.exitCode.get('tsc') !== 0) env.done(0, 'tsc'); // TODO - no more exitCode
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

// the splitConfig transform generates noCheck and noEmit tsconfigs within the 'build' temp folder.
// each workspace depndency package gets a --noCheck pass for fast declaration generation.
// then we do a --noEmit pass on EVERY package to verify things. the declarations from the --noCheck
// pass allow more efficient parallel type checking on most architectures

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

  if (error) throw new Error(`'${cfgPath}': ${error.messageText}`);

  const co: any = ts.parseJsonConfigFileContent(config, ts.sys, path.dirname(cfgPath)).options;

  if (co.moduleResolution) co.moduleResolution = ts.ModuleResolutionKind[co.moduleResolution];
  if (co.module) co.module = ts.ModuleKind[co.module];
  if (co.target) co.target = ts.ScriptTarget[co.target];

  co.lib = co.lib?.map((lib: string) => (lib.startsWith('lib.') ? lib.split('.')[1] : lib));
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
  config.references = env.workspaceDeps
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
  if (env.test && (await readable(path.join(root, 'tests')))) {
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
    noCheckData.compilerOptions.emitDeclarationOnly = true;
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

function tscLog(msg: string): void {
  env.log(msg, { ctx: 'tsc' });
}

function tscError(msg: Message): void {
  const { code, text, file, line, col } = msg.data;
  const prelude = `${errorMark} ts${code} `;
  const message = `${ts.flattenDiagnosticMessageText(text, '\n', 0)}`;
  let location = '';
  if (file) {
    location = `${c.grey('in')} '${c.cyan(path.relative(env.uiDir, file))}`;
    if (line !== undefined) location += c.grey(`:${line + 1}:${col + 1}`);
    location += `' - `;
  }
  tscLog(`${prelude}${location}${message}`);
  if (!env.exitCode.get('tsc')) env.done(1, 'tsc'); // TODO - no more exitCode
}
