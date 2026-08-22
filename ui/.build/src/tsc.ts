import fg from 'fast-glob';
import { spawn, type ChildProcess } from 'node:child_process';
import fs from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { createInterface } from 'node:readline';

import { env, c, errorMark, warnMark } from './env.ts';

let tscPs: ChildProcess | undefined;

export async function tsc(): Promise<void> {
  if (!env.begin('tsc')) return;
  await fs.promises.mkdir(env.buildTempDir, { recursive: true });

  const buildPaths = (await fg.glob('*/tsconfig*.json', { cwd: env.uiDir, absolute: true }))
    .sort((a, b) => a.localeCompare(b)) // repeatable build order
    .filter(x => env.building.some(pkg => x.startsWith(`${pkg.root}/`)));
  const configFile = relative(env.rootDir, join(env.buildTempDir, 'tsconfig.json'));
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

  await fs.promises.writeFile(
    configFile,
    JSON.stringify({ files: [], references: buildPaths.map(path => ({ path })) }),
  );
  env.log(
    `Typechecking ${c.grey('tsc --build')} '${c.cyan(configFile)}' ${c.grey(args.slice(7).join(' '))}`,
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
        tscLog(
          { file: loc[1], line: loc[2], col: loc[3], code: Number(loc[5]), text: loc[6] },
          loc[4] === 'warning',
        );
      } else if (global) {
        tscLog({ code: Number(global[2]), text: global[3] }, global[1] === 'warning');
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

interface Diagnostic {
  code: number;
  text: string;
  file?: string;
  line?: string;
  col?: string;
}

function tscLog({ code, text, file, line, col }: Diagnostic, isWarn = false): void {
  const prelude = `${isWarn ? warnMark : errorMark} ts${code} `;
  let loc = '';
  if (file) {
    loc = `${c.grey('in')} '${c.cyan(relative(env.uiDir, resolve(env.rootDir, file)))}`;
    if (line !== undefined) loc += c.grey(`:${line}:${col}`);
    loc += `' - `;
  }
  env.log(`${prelude}${loc}${text}`, 'tsc');
}
