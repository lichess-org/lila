#!/usr/bin/env node

import { readdirSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

process.chdir(dirname(dirname(fileURLToPath(import.meta.url))));

const packages = new Set(
  readdirSync('.', { withFileTypes: true })
    .filter(d => d.isDirectory() && !d.name.startsWith('.') && !d.name.startsWith('@'))
    .map(d => d.name),
);

const [dashArgs, globs] = getDashArgsAndGlobs();

const args = [
  '--test',
  ...dashArgs,
  '--experimental-test-module-mocks',
  '--no-warnings',
  '--loader',
  './.test/resolve.mts',
  '--import',
  'tsx',
  '--import',
  './.test/setup.mts',
  '--conditions=source',
  ...globs,
];

const child = spawn(process.execPath, args, { stdio: 'inherit' });
child.on('exit', code => process.exit(code ?? 1));
child.on('error', err => {
  console.error(err);
  process.exit(1);
});

function getDashArgsAndGlobs() {
  const argv = process.argv.slice(2);
  const dashArgs = argv.filter(x => x.startsWith('-')).map(x => (x === '-w' ? '--watch' : x));
  const posArgs = argv.filter(x => !x.startsWith('-'));
  const globs =
    posArgs.length === 0
      ? ['*/tests/**/*.ts']
      : posArgs.map(
          arg =>
            /^[A-Za-z0-9_]+$/.test(arg)
              ? packages.has(arg)
                ? `${arg}/tests/**/*.ts` // entire module
                : `*/tests/**/${arg}*.ts` // partial script name
              : /^[A-Za-z0-9_]+\.test\.ts$/.test(arg)
                ? `*/tests/**/${arg}` // exact script name
                : arg, // pass through user glob/path
        );
  return [dashArgs, globs];
}
