import ps from 'node:process';
import { deepClean, clean } from './clean.ts';
import { build, stopBuild } from './build.ts';
import { startConsole } from './console.ts';
import { env, errorMark } from './env.ts';
import { tasksIdle } from './task.ts';

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
  '--clean-exit': '',
  '--clean': 'c',
  '--no-install': 'n',
  '--log': 'l',
  '--kill': 'k',
};

const usage = `Usage:
  ui/build <options>  # multiple short options can follow a single dash

Options:
  -h, --help          show this help and exit
  -w, --watch         build and watch for changes
  -c, --clean         clean all build artifacts and build fresh
  -k, --kill          if another ui/build instance is running, kill it rather than bail
  -p, --prod          build minified assets (prod builds)
  -n, --no-install    don't run pnpm install
  -d, --debug         disable noUnusedLocals, noImplicitReturns, noUnusedParameters in tsc and build
                      assets with site.debug = true
  -l, --log=<url>     patch console logging functions in javascript manifest to POST messages to
                      <url> or localhost:8666 (default). if used with --watch, the watch process
                      will listen for http on 8666 and display received messages in build logs
  --clean-exit        clean all build artifacts and exit
  --no-color          don't use color in logs
  --no-time           don't log the time
  --no-context        don't log the context
  --no-corepack       don't use corepack to install pnpm (protect or restricted system node installs)

Exclusive Options:    (any of these will disable other functions)
  --tsc               run tsc on {package}/tsconfig.json and dependencies
  --sass              run sass on {package}/css/build/*.scss and dependencies
  --esbuild           run esbuild (given in {package}/package.json/lichess/bundles array)
  --i18n              build @types/lichess/i18n.d.ts and translation/js files

Recommended:
   ./build -w         # clean and watch for changes

Other Examples:
  ./build -np         # no pnpm install, build minified
  ./build --tsc -w    # watch mode but type checking only
  ./build -dwl=/xyz   # watch debug and patch console methods in emitted js to POST log statements
                        to \${location.origin}/xyz. ui/build watch process displays messages received
                        via http(s) on this endpoint as 'web' in build logs
`;

const argv = ps.argv.slice(2);
const oneDashRe = /^-([a-z]+)(?:=[a-zA-Z0-9-_:./]+)?$/;
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
  env.begin('sass', boolArg('--sass'));
  env.begin('tsc', boolArg('--tsc'));
  env.begin('esbuild', boolArg('--esbuild'));
  env.begin('i18n', boolArg('--i18n'));
  env.begin('sync', false);
  env.begin('hash', false);
}

env.logTime = !boolArg('--no-time');
env.logCtx = !boolArg('--no-context');
env.logColor = !boolArg('--no-color');
env.watch = boolArg('--watch');
env.prod = boolArg('--prod');
env.debug = boolArg('--debug');
env.remoteLog = stringArg('--log');
env.clean = boolArg('--clean');
env.install = !boolArg('--no-install');

if (boolArg('--help')) {
  console.log(usage);
  ps.exit(0);
}

if (!env.instanceLock(boolArg('--kill') ? 'kill' : 'check')) {
  env.exit(`${errorMark} - Another instance is already running`);
}

if (env.clean) {
  await deepClean();
  if (boolArg('--clean-exit')) ps.exit(0);
}

startConsole();

if (env.watch && 'setRawMode' in ps.stdin) {
  env.stdin = true;
  ps.stdin.setRawMode(true);
  ps.stdin.resume();
  ps.stdin.on('data', (key: Buffer) => {
    if (key[0] === 3 || key[0] === 27) {
      ps.exit(0);
    } else if (key[0] === 32 && tasksIdle()) {
      stopBuild().then(() => clean('force').then(() => build(argv.filter(x => !x.startsWith('-')))));
    }
  });
}

build(argv.filter(x => !x.startsWith('-')));

function getArg(longForm: string) {
  return argv.find(
    arg => arg.startsWith(longForm) || (args[longForm] && oneDashRe.exec(arg)?.[1]?.includes(args[longForm])),
  );
}
function stringArg(longForm: string): string | boolean {
  const it = getArg(longForm);
  return it?.split('=')[1] ?? (it ? true : false);
}

function boolArg(longForm: string): boolean {
  return getArg(longForm) !== undefined;
}
