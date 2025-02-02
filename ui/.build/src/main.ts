import ps from 'node:process';
import { deepClean } from './clean.ts';
import { build } from './build.ts';
import { startConsole } from './console.ts';
import { env, errorMark } from './env.ts';

// main entry point
['SIGINT', 'SIGTERM', 'SIGHUP'].forEach(sig => ps.on(sig, () => ps.exit(2)));

const args: Record<string, string> = {
  '--tsc': '',
  '--sass': '',
  '--esbuild': '',
  '--i18n': '',
  '--no-color': '',
  '--no-time': '',
  '--no-context': '',
  '--no-corepack': '',
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

const usage = `Usage:
  ui/build <options>  # multiple short options can be preceded by a single dash

Options:
  -h, --help          show this help and exit
  -w, --watch         build and watch for changes
  -c, --clean         clean all build artifacts and build fresh
  -p, --prod          build minified assets (prod builds)
  -n, --no-install    don't run pnpm install
  -d, --debug         build assets with site.debug = true
  -t, --test          typecheck sources in ./tests/**. warning - this enables skipLibCheck for dependencies
  -l, --log=<url>     monkey patch console log functions in javascript manifest to POST log messages to
                      <url> or localhost:8666 (default). if used with --watch, the ui/build process
                      will listen for http on 8666 and display received json as 'web' in build logs
  --update            update ui/.build/node_modules with pnpm install
  --no-color          don't use color in logs
  --no-time           don't log the time
  --no-context        don't log the context
  --no-corepack       don't use corepack to install pnpm (protect or restricted system node installs)

Exclusive Options:    (any of these will disable other functions)
  --tsc               run tsc on {package}/tsconfig.json and dependencies
  --sass              run sass on {package}/css/build/*.scss and dependencies
  --esbuild           run esbuild (given in {package}/package.json/lichess/bundles array)
  --i18n              build @types/lichess/i18n.d.ts and translation/js files
  --clean-exit        clean all build artifacts and exit

Recommended:
  ui/build -w         # clean and watch for changes

Other Examples:
  ./build -np         # no pnpm install, build minified
  ./build --tsc -w    # watch mode but type checking only
  ./build -dwl=/xyz   # build debug, watch. patch console methods in emitted js to POST log statements
                        to \${location.origin}/xyz. ui/build watch process displays messages received
                        via http(s) on this endpoint as 'web' in build logs
`;

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

if (['--tsc', '--sass', '--esbuild', '--i18n'].filter(x => argv.includes(x)).length) {
  // including one or more of these disables the others
  env.begin('sass', argv.includes('--sass'));
  env.begin('tsc', argv.includes('--tsc'));
  env.begin('esbuild', argv.includes('--esbuild'));
  env.begin('i18n', argv.includes('--i18n'));
  env.begin('sync', false);
  env.begin('hash', false);
}

env.logTime = !argv.includes('--no-time');
env.logCtx = !argv.includes('--no-context');
env.logColor = !argv.includes('--no-color');
env.watch = argv.includes('--watch') || oneDashArgs.includes('w');
env.prod = argv.includes('--prod') || oneDashArgs.includes('p');
env.debug = argv.includes('--debug') || oneDashArgs.includes('d');
env.remoteLog = stringArg('--log');
env.clean = argv.some(x => x.startsWith('--clean')) || oneDashArgs.includes('c');
env.install = !argv.includes('--no-install') && !oneDashArgs.includes('n');
env.rgb = argv.includes('--rgb');
env.test = argv.includes('--test') || oneDashArgs.includes('t');

if (argv.includes('--help') || oneDashArgs.includes('h')) {
  console.log(usage);
  ps.exit(0);
}

if (!env.instanceLock()) env.exit(`${errorMark} - Another instance is already running`);

if (env.clean) {
  await deepClean();
  if (argv.includes('--clean-exit')) ps.exit(0);
}

startConsole();

build(argv.filter(x => !x.startsWith('-')));
