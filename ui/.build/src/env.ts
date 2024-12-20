import path from 'node:path';
import type { Package } from './parse.ts';
import { unique, isEquivalent } from './algo.ts';
import { updateManifest } from './manifest.ts';

// state, logging, and exit code logic

type Builder = 'sass' | 'tsc' | 'esbuild';

export const env = new (class {
  readonly rootDir = path.resolve(path.dirname(new URL(import.meta.url).pathname), '../../..');
  readonly uiDir = path.join(this.rootDir, 'ui');
  readonly outDir = path.join(this.rootDir, 'public');
  readonly cssOutDir = path.join(this.outDir, 'css');
  readonly jsOutDir = path.join(this.outDir, 'compiled');
  readonly hashOutDir = path.join(this.outDir, 'hashed');
  readonly themeDir = path.join(this.uiDir, 'common', 'css', 'theme');
  readonly themeGenDir = path.join(this.themeDir, 'gen');
  readonly buildDir = path.join(this.uiDir, '.build');
  readonly cssTempDir = path.join(this.buildDir, 'build', 'css');
  readonly buildSrcDir = path.join(this.uiDir, '.build', 'src');
  readonly buildTempDir = path.join(this.buildDir, 'build');
  readonly typesDir = path.join(this.uiDir, '@types');
  readonly i18nSrcDir = path.join(this.rootDir, 'translation', 'source');
  readonly i18nDestDir = path.join(this.rootDir, 'translation', 'dest');
  readonly i18nJsDir = path.join(this.rootDir, 'translation', 'js');

  packages: Map<string, Package> = new Map();
  workspaceDeps: Map<string, string[]> = new Map();
  building: Package[] = [];

  watch = false;
  clean = false;
  prod = false;
  debug = false;
  remoteLog: string | boolean = false;
  rgb = false;
  install = true;
  sync = true;
  i18n = true;
  test = false;
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
    return (
      isEquivalent(this.building, [...this.packages.values()]) &&
      this.sync &&
      this.i18n &&
      (['tsc', 'esbuild', 'sass'] as const).every(x => this.exitCode.get(x) === 0)
    );
  }

  get manifestFile(): string {
    return path.join(this.jsOutDir, `manifest.${this.prod ? 'prod' : 'dev'}.json`);
  }

  transitiveDeps(pkgName: string): Package[] {
    const depList = (dep: string): string[] => [
      ...(this.workspaceDeps.get(dep) ?? []).flatMap(d => depList(d)),
      dep,
    ];
    return unique(depList(pkgName).map(name => this.packages.get(name)));
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
    this.log(colors.good('No errors') + this.watch ? ` - ${colors.grey('Watching')}...` : '', { ctx: ctx });
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
        { ctx },
      );
    if (allDone) {
      if (this.startTime && !err)
        this.log(`Done in ${colors.green((Date.now() - this.startTime) / 1000 + '')}s`);
      this.startTime = undefined; // it's pointless to time subsequent builds, they are too fast
    }
    if (!this.watch && err) process.exitCode = err;
    if (!err) updateManifest();
  }
})();

export const lines = (s: string): string[] => s.split(/[\n\r\f]+/).filter(x => x.trim());

const escape = (text: string, code: string): string => `\x1b[${code}m${stripColorEscapes(text)}\x1b[0m`;

const colorLines = (text: string, code: string) =>
  lines(text)
    .map(t => (env.color ? escape(t, code) : t))
    .join('\n');

const codes: Record<string, string> = {
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

export const errorMark: string = colors.red('✘ ') + colors.error('[ERROR]');
export const warnMark: string = colors.yellow('⚠ ') + colors.warn('[WARNING]');

const colorForCtx = (ctx: string, color: any): string =>
  color && ctx in color && color[ctx] in codes ? codes[color[ctx]] : codes.grey;

const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`);

function stripColorEscapes(text: string) {
  return text.replace(/\x1b\[[0-9;]*m/, '');
}

function prettyTime() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())} `;
}
