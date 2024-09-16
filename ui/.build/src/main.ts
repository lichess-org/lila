import ps from 'node:process';
import path from 'node:path';
import fs from 'node:fs';
import { clean } from './clean';
import { build, postBuild } from './build';

// readme should be up to date but this is the definitive list of flags
const args = [
  ['--tsc'],
  ['--sass'],
  ['--esbuild'],
  ['--copies'],
  ['--no-color'],
  ['--no-time'],
  ['--no-context'],
  ['--help'],
  ['--rebuild', '-r'],
  ['--watch', '-w'],
  ['--prod', '-p'],
  ['--debug', '-d'],
  ['--clean-build', '-c'],
  ['--clean'],
  ['--update'],
  ['--no-install', '-n'],
];

const longArgs = args.map(x => x[0]);
const shortArgs = args.map(x => x[1] && x[1][1]).filter(x => x);
type Builder = 'sass' | 'tsc' | 'esbuild';

export function main(): void {
  const args = ps.argv.slice(2);
  const oneDashArgs = args.filter(x => /^-([a-z]+)$/.test(x))?.flatMap(x => x.slice(1).split(''));
  oneDashArgs.filter(x => !shortArgs.includes(x)).forEach(arg => env.exit(`Unknown flag '-${arg}'`));
  args
    .filter(x => x.startsWith('--') && !longArgs.includes(x))
    .forEach(arg => env.exit(`Unknown argument '${arg}'`));

  if (['--tsc', '--sass', '--esbuild', '--copies'].filter(x => args.includes(x)).length) {
    // including one or more of these disables the others
    if (!args.includes('--sass')) env.exitCode.set('sass', false);
    if (!args.includes('--tsc')) env.exitCode.set('tsc', false);
    if (!args.includes('--esbuild')) env.exitCode.set('esbuild', false);
    env.copies = args.includes('--copies');
  }
  if (args.includes('--no-color')) env.color = undefined;
  if (args.includes('--no-time')) env.logTime = false;
  if (args.includes('--no-context')) env.logContext = false;

  env.rebuild = args.includes('--rebuild') || oneDashArgs.includes('r');
  env.watch = env.rebuild || args.includes('--watch') || oneDashArgs.includes('w');
  env.prod = args.includes('--prod') || oneDashArgs.includes('p');
  env.debug = args.includes('--debug') || oneDashArgs.includes('d');
  env.clean = args.some(x => x.startsWith('--clean')) || oneDashArgs.includes('c');
  env.install = !args.includes('--no-install') && !oneDashArgs.includes('n');
  env.rgb = args.includes('--rgb');

  if (env.rebuild && !env.install) {
    env.warn(`--rebuild incompatible with --no-install`);
    env.rebuild = false;
  }

  if (args.length === 1 && (args[0] === '--help' || args[0] === '-h')) {
    console.log(fs.readFileSync(path.resolve(__dirname, '../readme'), 'utf8'));
  } else if (args.includes('--clean')) {
    env.log('Cleaning then exiting. Use --clean-build or -c to clean then build');
    clean();
  } else {
    build(args.filter(x => !x.startsWith('-')));
  }
}

export interface Package {
  root: string; // absolute path to package.json parentdir (package root)
  name: string; // dirname of package root
  pkg: any; // the entire package.json object
  pre: string[][]; // pre-bundle build steps from package.json scripts
  post: string[][]; // post-bundle build steps from package.json scripts
  bundles?: string[];
  hashGlobs?: string[];
  sync?: Sync[]; // pre-bundle filesystem copies from package json
}

export interface Sync {
  // src must be a file or a glob expression, use <dir>/** to sync entire directories
  src: string;
  dest: string;
  pkg: Package;
}

export const lines = (s: string): string[] => s.split(/[\n\r\f]+/).filter(x => x.trim());

const colorLines = (text: string, code: string) =>
  lines(text)
    .map(t => (env.color ? escape(t, code) : t))
    .join('\n');

export const colors: Record<string, (text: string) => string> = {
  red: (text: string): string => colorLines(text, codes.red),
  green: (text: string): string => colorLines(text, codes.green),
  yellow: (text: string): string => colorLines(text, codes.yellow),
  blue: (text: string): string => colorLines(text, codes.blue),
  magenta: (text: string): string => colorLines(text, codes.magenta),
  cyan: (text: string): string => colorLines(text, codes.cyan),
  grey: (text: string): string => colorLines(text, codes.grey),
  black: (text: string): string => colorLines(text, codes.black),
  error: (text: string): string => colorLines(text, codes.error),
  warn: (text: string): string => colorLines(text, codes.warn),
  good: (text: string): string => colorLines(text, codes.green + ';1'),
  cyanBold: (text: string): string => colorLines(text, codes.cyan + ';1'),
};

class Env {
  rootDir: string = path.resolve(__dirname, '../../..'); // absolute path to lila project root

  deps: Map<string, string[]> = new Map();
  packages: Map<string, Package> = new Map();
  building: Package[] = [];

  watch = false;
  rebuild = false;
  clean = false;
  prod = false;
  debug = false;
  rgb = false;
  install = true;
  copies = true;
  exitCode: Map<Builder, number | false> = new Map();
  startTime: number | undefined = Date.now();
  logTime = true;
  logContext = true;
  color: any = {
    build: 'green',
    sass: 'magenta',
    tsc: 'yellow',
    esbuild: 'blue',
  };

  constructor() {}
  get sass(): boolean {
    return this.exitCode.get('sass') !== false;
  }
  get tsc(): boolean {
    return this.exitCode.get('tsc') !== false;
  }
  get esbuild(): boolean {
    return this.exitCode.get('esbuild') !== false;
  }
  get manifestOk(): boolean {
    return (['tsc', 'esbuild', 'sass'] as const).every(
      x => this.exitCode.get(x) === 0 || this.exitCode.get(x) === false,
    );
  }
  get uiDir(): string {
    return path.join(this.rootDir, 'ui');
  }
  get outDir(): string {
    return path.join(this.rootDir, 'public');
  }
  get cssOutDir(): string {
    return path.join(this.outDir, 'css');
  }
  get jsOutDir(): string {
    return path.join(this.outDir, 'compiled');
  }
  get hashOutDir(): string {
    return path.join(this.outDir, 'hashed');
  }
  get themeDir(): string {
    return path.join(this.uiDir, 'common', 'css', 'theme');
  }
  get themeGenDir(): string {
    return path.join(this.themeDir, 'gen');
  }
  get buildDir(): string {
    return path.join(this.uiDir, '.build');
  }
  get cssTempDir(): string {
    return path.join(this.buildDir, '.build-gen', 'css');
  }
  get buildTempDir(): string {
    return path.join(this.buildDir, 'build');
  }
  get typesDir(): string {
    return path.join(this.uiDir, '@types');
  }
  get manifestFile(): string {
    return path.join(this.jsOutDir, `manifest.${this.prod ? 'prod' : 'dev'}.json`);
  }
  warn(d: any, ctx = 'build'): void {
    this.log(d, { ctx: ctx, warn: true });
  }
  error(d: any, ctx = 'build'): void {
    this.log(d, { ctx: ctx, error: true });
  }
  exit(d: any, ctx = 'build'): void {
    this.log(d, { ctx: ctx, error: true });
    process.exit(1);
  }
  good(ctx = 'build'): void {
    this.log(colors.good('No errors') + env.watch ? ` - ${colors.grey('Watching')}...` : '', { ctx: ctx });
  }
  log(d: any, { ctx = 'build', error = false, warn = false }: any = {}): void {
    let text: string =
      !d || typeof d === 'string' || d instanceof Buffer
        ? String(d)
        : Array.isArray(d)
          ? d.join('\n')
          : JSON.stringify(d);

    const esc = this.color ? escape : (text: string, _: any) => text;

    if (!this.color) text = stripColorEscapes(text);

    const prefix = (
      (this.logTime === false ? '' : prettyTime()) +
      (!ctx || !this.logContext ? '' : `[${esc(ctx, colorForCtx(ctx, this.color))}] `)
    ).trim();

    lines(text).forEach(line =>
      console.log(
        `${prefix ? prefix + ' - ' : ''}${
          error ? esc(line, codes.error) : warn ? esc(line, codes.warn) : line
        }`,
      ),
    );
  }
  done(code: number, ctx: Builder): void {
    this.exitCode.set(ctx, code);
    const err = [...this.exitCode.values()].find(x => x);
    const allDone = this.exitCode.size === 3;
    if (ctx !== 'tsc' || code === 0)
      this.log(
        `${code === 0 ? 'Done' : colors.red('Failed')}` +
        (this.watch ? ` - ${colors.grey('Watching')}...` : ''),
        {
          ctx: ctx,
        },
      );
    if (allDone) {
      if (!err) postBuild();
      if (this.startTime && !err)
        this.log(`Done in ${colors.green((Date.now() - this.startTime) / 1000 + '')}s`);
      this.startTime = undefined; // it's pointless to time subsequent builds, they are too fast
    }
    if (!env.watch && err) {
      process.exitCode = err;
    }
  }
}

export const env: Env = new Env();

export const codes: Record<string, string> = {
  black: '30',
  red: '31',
  green: '32',
  yellow: '33',
  blue: '34',
  magenta: '35',
  cyan: '36',
  grey: '90',
  error: '31',
  warn: '33',
};

const colorForCtx = (ctx: string, color: any): string =>
  color && ctx in color && color[ctx] in codes ? codes[color[ctx]] : codes.grey;

const escape = (text: string, code: string): string => `\x1b[${code}m${stripColorEscapes(text)}\x1b[0m`;

const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`);

function stripColorEscapes(text: string) {
  return text.replace(/\x1b\[[0-9;]*m/, '');
}

export const errorMark: string = colors.red('✘ ') + colors.error('[ERROR]');
export const warnMark: string = colors.yellow('⚠ ') + colors.warn('[WARNING]');

function prettyTime() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())} `;
}

main();
