import fg from 'fast-glob';
import { parse, printParseErrorCode, type ParseError } from 'jsonc-parser';
import { spawn, type ChildProcess } from 'node:child_process';
import fs from 'node:fs';
import { basename, dirname, join, relative, resolve } from 'node:path';
import { createInterface } from 'node:readline';
import pc from 'picocolors';

import { env, errorMark, warnMark } from './env.ts';
import { i18nTypingsReady } from './i18n.ts';

let tscPs: ChildProcess | undefined;

export async function tsc(): Promise<void> {
  if (!env.begin('tsc')) return;
  await Promise.all([i18nTypingsReady.promise, fs.promises.mkdir(env.buildTempDir, { recursive: true })]);

  const buildPaths = (await fg.glob('*/tsconfig*.json', { cwd: env.uiDir, absolute: true }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));
  const configFile = relative(env.rootDir, await makeTsconfigs(buildPaths));
  const args = [
    join(relative(env.rootDir, env.buildDir), 'node_modules', 'typescript', 'bin', 'tsc'),
    '--build',
    configFile,
    '--pretty',
    'false',
    '--locale',
    'en', // watch mode needs 'Found <n> errors'
  ];
  if (env.watch) args.push('--watch', '--preserveWatchOutput');

  env.log(
    `Type checking ${pc.gray('tsc --build')} '${pc.cyan(configFile)}' ${pc.gray(args.slice(3).join(' '))}`,
    'tsc',
  );

  return new Promise((resolve, reject) => {
    const childPs = spawn(process.execPath, args, { cwd: env.rootDir, stdio: 'pipe' });
    tscPs = childPs;

    const onLine = (raw: string) => {
      const line = raw
        .replace(/\x1b(?:\[[0-?]*[ -/]*[@-~]|c)/g, '') // strip escapes
        .replace(/^\d{1,2}:\d{2}:\d{2} (?:AM|PM) - /, '') // strip time
        .trimEnd();
      const loc = /^(.*)\((\d+),(\d+)\): (error|warning) TS(\d+): (.*)$/.exec(line);
      const global = /^(error|warning) TS(\d+): (.*)$/.exec(line);
      if (loc) {
        tscLog({
          file: loc[1],
          line: loc[2],
          col: loc[3],
          warning: loc[4] === 'warning',
          code: Number(loc[5]),
          text: loc[6],
        });
      } else if (global) {
        tscLog({ warning: global[1] === 'warning', code: Number(global[2]), text: global[3] });
      } else {
        const summary = /^Found (\d+) errors?\./.exec(line);
        if (summary) {
          env.setStatus('tsc', Number(summary[1]) > 0 ? -2 : 0);
          if (env.status.tsc === 0) resolve();
        } else if (line.includes('File change detected')) {
          env.setStatus('tsc', undefined);
        } else if (line && !line.includes('Starting compilation')) env.log(line, 'tsc');
      }
    };

    createInterface({ input: childPs.stdout }).on('line', onLine);
    createInterface({ input: childPs.stderr }).on('line', onLine);
    childPs.on('error', reject);
    childPs.on('close', code => {
      if (tscPs !== childPs) return resolve();
      tscPs = undefined;
      if (!code && env.status.tsc !== -2) {
        env.setStatus('tsc', 0);
        resolve();
      } else {
        env.setStatus('tsc', -2);
        reject(new Error(`TypeScript exited with code ${code}`));
      }
    });
  });
}

export async function stopTsc(): Promise<void> {
  const killPs = tscPs;
  tscPs = undefined;
  if (killPs?.exitCode !== null) return;
  await new Promise<void>(resolve => {
    killPs.once('close', () => resolve());
    killPs.kill();
  });
}

type Tsconfig = {
  // type Tsconfig is temporary and should be removed once the tsc 7.1 api lands
  [option: string]: any;
  references?: { path: string }[];
  compilerOptions?: Record<string, any> & { paths?: Record<string, string[]> };
};

async function makeTsconfigs(paths: string[]): Promise<string> {
  const basePath = join(env.uiDir, 'tsconfig.base.json');
  const generated = (path: string) => join(env.buildTempDir, `${basename(dirname(path))}.${basename(path)}`);
  const [, base, originals] = await Promise.all([
    fs.promises.mkdir(env.buildTempDir, { recursive: true }),
    readTsconfig(basePath),
    Promise.all(paths.map(readTsconfig)),
  ]);
  const writes: Promise<void>[] = [];
  for (const [index, path] of paths.entries()) {
    const original = originals[index];
    const moduleRoot = dirname(path);
    const moduleName = basename(moduleRoot);
    const inherited = [original.extends ?? []].flat().some(parent => resolve(moduleRoot, parent) === basePath)
      ? base
      : {};
    const relocate = (value: string) => resolve(moduleRoot, value.replaceAll('${configDir}', moduleRoot));
    const options: Tsconfig['compilerOptions'] = {};
    const effective = { ...inherited.compilerOptions, ...original.compilerOptions };
    for (const key of ['baseUrl', 'rootDir', 'outDir', 'declarationDir'] as const) {
      if (effective[key]?.includes('${configDir}')) options[key] = relocate(effective[key]);
    }
    for (const key of ['rootDirs', 'typeRoots'] as const) {
      if (effective[key]?.some((value: string) => value.includes('${configDir}')))
        options[key] = effective[key].map(relocate);
    }
    if (effective.paths)
      options.paths = Object.fromEntries(
        Object.entries(effective.paths).map(([alias, targets]) => [alias, targets.map(relocate)]),
      );
    const config: Tsconfig = { extends: path, compilerOptions: options };
    for (const key of ['files', 'include', 'exclude'] as const) {
      const value = original[key] ?? inherited[key];
      if (value?.some((entry: string) => entry.includes('${configDir}'))) config[key] = value.map(relocate);
    }
    if (!original.files && !original.include && !inherited.include)
      config.include = [join(moduleRoot, '**/*')];
    const mirrored = new Set(
      original.references?.map(reference => {
        const target = resolve(moduleRoot, reference.path);
        return target.endsWith('.json') ? target : join(target, 'tsconfig.json');
      }),
    );
    const dependencies = (env.workspaceDeps.get(moduleName) ?? []).map(name =>
      join(env.uiDir, name, 'tsconfig.json'),
    );
    const fileLink = (file: string) => `'${pc.cyan(relative(env.rootDir, file))}'`;
    for (const dependency of dependencies) {
      if (!mirrored.has(dependency))
        env.log(`${warnMark} ${fileLink(path)} must reference ${fileLink(dependency)}`, 'tsc');
    }
    for (const dependency of mirrored) {
      if (!dependencies.includes(dependency))
        env.log(`${warnMark} ${fileLink(path)} must not reference ${fileLink(dependency)}`, 'tsc');
    }
    const packageName = env.packages.get(moduleName)?.pkg.name ?? moduleName;
    if (!options.typeRoots && !effective.typeRoots)
      options.typeRoots = [
        join(moduleRoot, 'node_modules/@types'),
        join(env.uiDir, 'node_modules/@types'),
        join(env.rootDir, 'node_modules/@types'),
      ];
    if (options.rootDir) {
      options.paths = {
        ...options.paths,
        [`${packageName}/*`]: [join(options.rootDir, '*')],
      };
    }
    options.tsBuildInfoFile = generated(path).replace(/\.json$/, '.tsbuildinfo');
    if (env.debug)
      Object.assign(options, {
        noUnusedLocals: false,
        noUnusedParameters: false,
        noImplicitReturns: false,
        allowUnreachableCode: true,
      });
    config.references = dependencies.map(path => ({ path: generated(path) }));
    writes.push(writeTsconfig(generated(path), config));
  }
  const solution = join(env.buildTempDir, 'tsconfig.json');
  await Promise.all([
    ...writes,
    writeTsconfig(solution, { files: [], references: paths.map(path => ({ path: generated(path) })) }),
  ]);
  return solution;
}

async function readTsconfig(path: string): Promise<Tsconfig> {
  const text = await fs.promises.readFile(path, 'utf8');
  const errors: ParseError[] = [];
  const config = parse(text, errors, { allowTrailingComma: true }) as Tsconfig;
  if (errors.length)
    throw new Error(`${path} at offset ${errors[0].offset}: ${printParseErrorCode(errors[0].error)}`);
  return config;
}

async function writeTsconfig(path: string, config: Tsconfig): Promise<void> {
  const text = JSON.stringify(config);
  if (text !== (await fs.promises.readFile(path, 'utf8').catch(() => undefined)))
    await fs.promises.writeFile(path, text);
}

interface Diagnostic {
  code: number;
  text: string;
  file?: string;
  line?: string;
  col?: string;
  warning?: boolean;
}

function tscLog({ code, text, file, line, col, warning }: Diagnostic): void {
  const prelude = `${warning ? warnMark : errorMark} ts${code} `;
  let loc = '';
  if (file) {
    loc = `${pc.gray('in')} '${pc.cyan(relative(env.uiDir, resolve(env.rootDir, file)))}`;
    if (line !== undefined) loc += pc.gray(`:${line}:${col}`);
    loc += `' - `;
  }
  env.log(`${prelude}${loc}${text}`, 'tsc');
}
