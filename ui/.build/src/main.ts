import ps from 'node:process';
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

Exclusive Options:    (any of these will disable other functions)
  --clean-exit        clean all build artifacts and exit
  --tsc               run tsc on {package}/tsconfig.json and dependencies
  --sass              run sass on {package}/css/build/*.scss and dependencies
  --esbuild           run esbuild (given in {package}/package.json/lichess/bundles array)
  --sync              run sync copies (given in {package}/package.json/lichess/sync objects)
  --i18n              build @types/lichess/i18n.d.ts and translation/js files

Recommended:
  ui/build -cdw       # clean, build debug, and watch for changes with clean rebuilds

Other Examples:
  ./build -np         # no pnpm install, build minified
  ./build --tsc -w    # watch mode but type checking only
  ./build -dwl=/x     # build debug, watch. patch console methods in emitted js to POST log statements
                        to \${location.origin}/x. ui/build watch process displays messages received
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
  console.log(usage);
  process.exit(0);
} else if (env.clean) {
  await deepClean();
  if (argv.includes('--clean-exit')) process.exit(0);
}
startConsole();
build(argv.filter(x => !x.startsWith('-')));
