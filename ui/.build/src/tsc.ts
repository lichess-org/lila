import fs from 'node:fs';
import os from 'node:os';
import { join, resolve, dirname, basename, relative } from 'node:path';
import ts from 'typescript';
import fg from 'fast-glob';
import { Worker } from 'node:worker_threads';
import { env, c, errorMark } from './env.ts';
import { folderSize } from './parse.ts';
import { clamp } from './algo.ts';
import type { WorkerData, Message, ErrorMessage } from './tscWorker.ts';

const workers: Worker[] = [];
const spamGuard = new Map<string, number>(); // dedup tsc errors

export async function tsc(): Promise<void> {
  if (!env.begin('tsc')) return;
  await Promise.allSettled([
    fs.promises.mkdir(join(env.buildTempDir, 'noCheck')),
    fs.promises.mkdir(join(env.buildTempDir, 'noEmit')),
  ]);

  const buildPaths = (await fg.glob('*/tsconfig*.json', { cwd: env.uiDir, absolute: true }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));

  const logicalCores = os.cpus().length;
  const workBuckets: Record<'noCheck' | 'noEmit', SplitConfig[][]> = {
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

  env.log(`Typing ${c.grey('--noCheck')} (${workBuckets.noCheck.length} workers)`, 'tsc');
  await assignWork(workBuckets.noCheck, 'noCheck');

  env.log(`Checking ${c.grey('--noEmit')} (${workBuckets.noEmit.length} workers)`, 'tsc');
  await assignWork(workBuckets.noEmit, 'noEmit');
}

export async function stopTsc(): Promise<void> {
  await Promise.allSettled(workers.map(w => w.terminate()));
  workers.length = 0;
}

function assignWork(buckets: SplitConfig[][], key: 'noCheck' | 'noEmit'): Promise<void> {
  let okResolve: () => void;
  const status: ('ok' | 'busy' | 'error')[] = [];
  const okPromise = new Promise<void>(res => (okResolve = res));
  const onError = (err: Error): void => env.exit(err.message, 'tsc');
  const onMessage = (msg: Message): void => {
    // direct error -> ok transition must be ignored. error must transition to busy first
    if (env.watch && msg.type === 'ok' && status[msg.index] === 'error') return;

    status[msg.index] = msg.type;

    switch (msg.type) {
      case 'error':
        return tscError(msg.data);
      case 'busy':
        return env.done('tsc', undefined);
      case 'ok':
        if (status.some(s => s !== 'ok')) return;
        if (key === 'noEmit') env.done('tsc', 0);
        okResolve();
    }
  };

  for (const bucket of buckets) {
    const workerData: WorkerData = {
      projects: bucket.map(p => p.configFile),
      watch: env.watch,
      index: status.length,
    };
    status.push('busy');
    const worker = new Worker(resolve(env.buildSrcDir, 'tscWorker.ts'), { workerData });
    workers.push(worker);
    worker.on('message', onMessage);
    worker.on('error', onError);
  }
  return okPromise;
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
  const root = dirname(cfgPath);
  const pkgName = basename(root);
  const { config, error } = ts.readConfigFile(cfgPath, ts.sys.readFile);
  const io: Promise<any>[] = [];
  const fixGlobs = (globs?: string[]) => globs?.map(glob => resolve(root, glob.replace('${configDir}', '.')));

  if (error) throw new Error(`'${cfgPath}': ${error.messageText}`);

  const co: any = ts.parseJsonConfigFileContent(config, ts.sys, dirname(cfgPath)).options;

  if (co.moduleResolution) co.moduleResolution = ts.ModuleResolutionKind[co.moduleResolution];
  if (co.module) co.module = ts.ModuleKind[co.module];
  if (co.target) co.target = ts.ScriptTarget[co.target];
  if (env.debug) {
    co.noUnusedLocals = false;
    co.noImplicitReturns = false;
    co.noUnusedParameters = false;
  }
  co.lib = co.lib?.map((lib: string) => lib.match(/lib\.(.+)\.d\.ts/)?.[1] ?? lib);
  co.pathsBasePath = undefined;
  co.incremental = true;
  config.compilerOptions = co;
  config.size = await folderSize(join(root, 'src'));
  config.include = fixGlobs(config.include) ?? [co.rootDir ? `${co.rootDir}/**/*` : `${root}/src/**/*`];
  config.exclude = fixGlobs(config.exclude);
  config.extends = undefined;
  config.references = env.workspaceDeps
    .get(pkgName)
    ?.map(ref => ({ path: join(env.buildTempDir, 'noCheck', `${ref}.tsconfig.json`) }));

  const checkCfg = structuredClone(config);
  const checkPath = join(env.buildTempDir, 'noEmit', `${pkgName}.tsconfig.json`);
  checkCfg.compilerOptions.noEmit = true;
  checkCfg.compilerOptions.tsBuildInfoFile = join(env.buildTempDir, 'noEmit', `${pkgName}.tsbuildinfo`);
  io.push(fs.promises.writeFile(checkPath, JSON.stringify(checkCfg, undefined, 2)));

  const res: SplitConfig[] = [{ type: 'noEmit', configFile: checkPath, pkgName, size: config.size }];

  if (!co.noEmit) {
    const emitCfg = structuredClone(config);
    const emitPath = join(env.buildTempDir, 'noCheck', `${pkgName}.tsconfig.json`);
    emitCfg.compilerOptions.noCheck = true;
    emitCfg.compilerOptions.emitDeclarationOnly = true;
    emitCfg.compilerOptions.tsBuildInfoFile = join(env.buildTempDir, 'noCheck', `${pkgName}.tsbuildinfo`);
    res.push({ type: 'noCheck', configFile: emitPath, pkgName, size: config.size });
    io.push(fs.promises.writeFile(emitPath, JSON.stringify(emitCfg, undefined, 2)));
  }
  await Promise.all(io);
  return res;
}

function tscError({ code, text, file, line, col }: ErrorMessage['data']): void {
  const key = `${code}:${text}:${file}`;
  if (performance.now() > (spamGuard.get(key) ?? -Infinity)) {
    const prelude = `${errorMark} ts${code} `;
    const message = ts.flattenDiagnosticMessageText(text, '\n', 0);
    let location = '';
    if (file) {
      location = `${c.grey('in')} '${c.cyan(relative(env.uiDir, file))}`;
      if (line !== undefined) location += c.grey(`:${line + 1}:${col + 1}`);
      location += `' - `;
    }
    env.log(`${prelude}${location}${message}`, 'tsc');
  }
  spamGuard.set(key, performance.now() + 1000);
  env.done('tsc', -2);
}
