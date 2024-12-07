import ps from 'node:process';
import path from 'node:path';
import fs from 'node:fs';
import { deepClean } from './clean.ts';
import { build } from './build.ts';
import { startConsole } from './console.ts';
import { env } from './env.ts';

// process arguments and kick off the build
// it more or less fits on one page so no need for an args library

// readme should be up to date but this is the definitive list of flags
const args: Record<string, string> = {
  '--tsc': '',
  '--sass': '',
  '--esbuild': '',
  '--sync': '',
  '--i18n': '',
  '--no-color': '',
  '--no-time': '',
  '--no-context': '',
  '--help': 'h',
  '--watch': 'w',
  '--prod': 'p',
  '--debug': 'd',
  '--test': 't',
  '--clean-exit': '',
  '--clean': 'c',
  '--no-install': 'n',
  '--log': 'l',
};
const argv = ps.argv.slice(2);
const oneDashRe = /^-([a-z]+)(?:=[a-zA-Z0-9-_:./]+)?$/;

const stringArg = (arg: string): string | boolean => {
  const it = argv.find(x => x.startsWith(arg) || (args[arg] && oneDashRe.exec(x)?.[1]?.includes(args[arg])));
  return it?.split('=')[1] ?? (it ? true : false);
};

const oneDashArgs = argv
  .flatMap(x => oneDashRe.exec(x)?.[1] ?? '')
  .join('')
  .split('');

oneDashArgs.filter(x => !Object.values(args).includes(x)).forEach(arg => env.exit(`Unknown flag '-${arg}'`));

argv
  .filter(x => x.startsWith('--') && !Object.keys(args).includes(x.split('=')[0]))
  .forEach(arg => env.exit(`Unknown argument '${arg}'`));

if (['--tsc', '--sass', '--esbuild', '--sync', '--i18n'].filter(x => argv.includes(x)).length) {
  // including one or more of these disables the others
  if (!argv.includes('--sass')) env.exitCode.set('sass', false);
  if (!argv.includes('--tsc')) env.exitCode.set('tsc', false);
  if (!argv.includes('--esbuild')) env.exitCode.set('esbuild', false);
  env.i18n = argv.includes('--i18n');
  env.sync = argv.includes('--sync');
}
if (argv.includes('--no-color')) env.color = undefined;

env.logTime = !argv.includes('--no-time');
env.logContext = !argv.includes('--no-context');
env.watch = argv.includes('--watch') || oneDashArgs.includes('w');
env.prod = argv.includes('--prod') || oneDashArgs.includes('p');
env.debug = argv.includes('--debug') || oneDashArgs.includes('d');
env.remoteLog = stringArg('--log');
env.clean = argv.some(x => x.startsWith('--clean')) || oneDashArgs.includes('c');
env.install = !argv.includes('--no-install') && !oneDashArgs.includes('n');
env.rgb = argv.includes('--rgb');
env.test = argv.includes('--test') || oneDashArgs.includes('t');

if (argv.length === 1 && (argv[0] === '--help' || argv[0] === '-h')) {
  console.log(fs.readFileSync(path.resolve(env.buildDir, 'usage.txt'), 'utf8'));
  process.exit(0);
} else if (env.clean) {
  await deepClean();
  if (argv.includes('--clean-exit')) process.exit(0);
}
startConsole();
build(argv.filter(x => !x.startsWith('-')));
