import p from 'node:path';
import type { Package } from './parse.ts';
import { unique, isEquivalent, trimLines } from './algo.ts';
import { updateManifest } from './manifest.ts';
import { taskOk } from './task.ts';

// state, logging, status

export const env = new (class {
  readonly rootDir = p.resolve(p.dirname(new URL(import.meta.url).pathname), '../../..');
  readonly uiDir = p.join(this.rootDir, 'ui');
  readonly outDir = p.join(this.rootDir, 'public');
  readonly cssOutDir = p.join(this.outDir, 'css');
  readonly jsOutDir = p.join(this.outDir, 'compiled');
  readonly hashOutDir = p.join(this.outDir, 'hashed');
  readonly themeDir = p.join(this.uiDir, 'common', 'css', 'theme');
  readonly themeGenDir = p.join(this.themeDir, 'gen');
  readonly buildDir = p.join(this.uiDir, '.build');
  readonly cssTempDir = p.join(this.buildDir, 'build', 'css');
  readonly buildSrcDir = p.join(this.uiDir, '.build', 'src');
  readonly buildTempDir = p.join(this.buildDir, 'build');
  readonly typesDir = p.join(this.uiDir, '@types');
  readonly i18nSrcDir = p.join(this.rootDir, 'translation', 'source');
  readonly i18nDestDir = p.join(this.rootDir, 'translation', 'dest');
  readonly i18nJsDir = p.join(this.rootDir, 'translation', 'js');

  watch = false;
  clean = false;
  prod = false;
  debug = false;
  rgb = false;
  test = false;
  install = true;
  logTime = true;
  logCtx = true;
  logColor = true;
  remoteLog: string | boolean = false;
  startTime: number | undefined = Date.now();

  packages: Map<string, Package> = new Map();
  workspaceDeps: Map<string, string[]> = new Map();
  building: Package[] = [];

  private status: { [key in Context]?: number | false } = {};

  get manifest(): boolean {
    return (
      isEquivalent(this.building, [...this.packages.values()]) &&
      (['tsc', 'esbuild', 'sass', 'i18n'] as const).map(b => this.status[b]).every(x => x === 0)
    );
  }

  get manifestFile(): string {
    return p.join(this.jsOutDir, `manifest.${this.prod ? 'prod' : 'dev'}.json`);
  }

  *tasks<T extends 'sync' | 'hash' | 'bundle'>(
    t: T,
  ): Generator<[Package, Package[T] extends Array<infer U> ? U : never]> {
    for (const pkg of this.building) {
      for (const item of pkg[t] as (Package[T] extends (infer U)[] ? U : never)[]) {
        yield [pkg, item];
      }
    }
  }

  deps(pkgName: string): Package[] {
    const depList = (dep: string): string[] => [
      ...(this.workspaceDeps.get(dep) ?? []).flatMap(d => depList(d)),
      dep,
    ];
    return unique(depList(pkgName).map(name => this.packages.get(name)));
  }

  log(d: any, ctx = 'build'): void {
    const text: string =
      !d || typeof d === 'string' || d instanceof Buffer
        ? String(d)
        : Array.isArray(d)
          ? d.join('\n')
          : JSON.stringify(d);

    const prefix = (
      (this.logTime ? prettyTime() : '') + (ctx && this.logCtx ? `[${escape(ctx, colorForCtx(ctx))}]` : '')
    ).trim();

    for (const line of trimLines(this.logColor ? text : stripColorEscapes(text)))
      console.log(`${prefix ? prefix + ' - ' : ''}${line}`);
  }

  exit(d?: any, ctx = 'build'): void {
    if (d) this.log(d, ctx);
    process.exit(1);
  }

  begin(ctx: Context, enable?: boolean): boolean {
    if (enable === false) this.status[ctx] = false;
    else if (enable === true || this.status[ctx] !== false) this.status[ctx] = undefined;
    return this.status[ctx] !== false;
  }

  done(ctx: Context, code: number = 0): void {
    if (code !== this.status[ctx] && ['tsc', 'esbuild', 'sass', 'i18n'].includes(ctx)) {
      this.log(
        `${code === 0 ? 'Done' : c.red('Failed')}` + (this.watch ? ` - ${c.grey('Watching')}...` : ''),
        ctx,
      );
    }
    this.status[ctx] = code;
    if (this.manifest && taskOk()) {
      if (this.startTime) this.log(`Done in ${c.green((Date.now() - this.startTime) / 1000 + '')}s`);
      this.startTime = undefined;
      updateManifest();
    }
    if (!this.watch && code) process.exit(code);
  }
})();

export type Context = 'sass' | 'tsc' | 'esbuild' | 'sync' | 'hash' | 'i18n' | 'web';

const codes = {
  black: '30',
  red: '31',
  green: '32',
  yellow: '33',
  blue: '34',
  magenta: '35',
  cyan: '36',
  white: '37',
  grey: '90',
  error: '31',
  warn: '33',
  greenBold: '32;1',
  yellowBold: '33;1',
  blueBold: '34;1',
  magentaBold: '35;1',
  cyanBold: '36;1',
  greyBold: '90;1',
};

function colorForCtx(ctx: string) {
  return (
    {
      build: codes.green,
      sass: codes.magenta,
      tsc: codes.yellow,
      esbuild: codes.blueBold,
      sync: codes.cyan,
      hash: codes.blue,
      i18n: codes.cyanBold,
      manifest: codes.white,
      web: codes.magentaBold,
    }[ctx] ?? codes.grey
  );
}

function escape(text: string, code?: string) {
  return env.logColor && code ? `\x1b[${code}m${stripColorEscapes(text)}\x1b[0m` : text;
}

function colorLines(text: string, code: string) {
  return trimLines(text)
    .map(t => escape(t, code))
    .join('\n');
}

export const c: Record<keyof typeof codes, (text: string) => string> = Object.keys(codes).reduce(
  (acc, key) => {
    acc[key as keyof typeof codes] = (text: string) => colorLines(text, codes[key as keyof typeof codes]);
    return acc;
  },
  {} as Record<keyof typeof codes, (text: string) => string>,
);

export const errorMark: string = c.red('✘ ') + c.error('[ERROR]');
export const warnMark: string = c.yellow('⚠ ') + c.warn('[WARNING]');

function pad2(n: number) {
  return n < 10 ? `0${n}` : `${n}`;
}

function stripColorEscapes(text: string) {
  return text.replace(/\x1b\[[0-9;]*m/, '');
}

function prettyTime() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())} `;
}
