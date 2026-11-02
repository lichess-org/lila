#!/usr/bin/env node

import { spawn } from 'node:child_process';
import { readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

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
  '--import',
  'tsx',
  '--import',
  './.test/resolve.mts',
  '--import',
  './.test/setup.mts',
  '--conditions=source',
  ...globs,
];

// Each test file runs in its own process; the cache spares them all recompiling jsdom and friends.
const env = {
  NODE_COMPILE_CACHE: join(tmpdir(), 'node-compile-cache'),
  TSX_TSCONFIG_PATH: join(process.cwd(), 'tsconfig.base.json'),
  ...process.env,
};
const child = spawn(process.execPath, args, { stdio: 'inherit', env });
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
