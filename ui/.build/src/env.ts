import type { Package } from './parse.ts';
import fs from 'node:fs';
import ps from 'node:process';
import { join, resolve, dirname } from 'node:path';
import { definedUnique, isEquivalent } from './algo.ts';
import { updateManifest } from './manifest.ts';
import { taskOk } from './task.ts';

// state, logging, status

export const env = new (class {
  readonly rootDir = resolve(dirname(new URL(import.meta.url).pathname), '../../..');
  readonly uiDir = join(this.rootDir, 'ui');
  readonly outDir = join(this.rootDir, 'public');
  readonly cssOutDir = join(this.outDir, 'css');
  readonly jsOutDir = join(this.outDir, 'compiled');
  readonly hashOutDir = join(this.outDir, 'hashed');
  readonly themeDir = join(this.uiDir, 'lib', 'css', 'theme');
  readonly themeGenDir = join(this.themeDir, 'gen');
  readonly buildDir = join(this.uiDir, '.build');
  readonly lockFile = join(this.buildDir, 'instance.lock');
  readonly buildTempDir = join(this.buildDir, 'build');
  readonly cssTempDir = join(this.buildTempDir, 'css');
  readonly buildSrcDir = join(this.buildDir, 'src');
  readonly typesDir = join(this.uiDir, '@types');
  readonly i18nSrcDir = join(this.rootDir, 'translation', 'source');
  readonly i18nDestDir = join(this.rootDir, 'translation', 'dest');
  readonly i18nJsDir = join(this.rootDir, 'translation', 'js');

  watch = false;
  clean = false;
  prod = false;
  debug = false;
  stdin = false;
  install = true;
  logTime = true;
  logCtx = true;
  logColor = true;
  remoteLog: string | boolean = false;
  startTime: number | undefined;

  packages: Map<string, Package> = new Map();
  workspaceDeps: Map<string, string[]> = new Map();
  building: Package[] = [];

  private status: { [key in Context]?: number | false } = {};

  manifestOk(): boolean {
    return (
      isEquivalent(this.building, [...this.packages.values()]) &&
      (['tsc', 'esbuild', 'sass', 'i18n'] as const).map(b => this.status[b]).every(x => x === 0)
    );
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
    return definedUnique(depList(pkgName).map(name => this.packages.get(name)));
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

  done(ctx: Context, code: number | undefined): void {
    if (code !== undefined && code !== this.status[ctx] && ['tsc', 'esbuild', 'sass', 'i18n'].includes(ctx)) {
      this.log(
        `${code === 0 ? 'Done' : c.red('Failed')}` + (this.watch ? ` - ${c.grey('Watching')}...` : ''),
        ctx,
      );
    }
    this.status[ctx] = code;
    if (this.manifestOk() && taskOk()) {
      if (this.startTime) {
        const doneMsg = `Done in ${c.green((Date.now() - this.startTime) / 1000 + '')}s`;
        this.log(
          doneMsg + (this.stdin ? `. Press ${c.grey('<space>')} to clean, ${c.grey('<esc>')} to exit` : ''),
        );
      }
      updateManifest();
      this.startTime = undefined;
    }
    if (!this.watch && code) process.exit(code);
  }

  instanceLock(checkStale = true): boolean {
    try {
      const fd = fs.openSync(env.lockFile, 'wx');
      fs.writeFileSync(fd, String(ps.pid), { flag: 'w' });
      fs.closeSync(fd);
      ps.on('exit', () => fs.unlinkSync(env.lockFile));
    } catch {
      const pid = parseInt(fs.readFileSync(env.lockFile, 'utf8'), 10);
      if (!isNaN(pid) && pid > 0 && ps.platform !== 'win32') {
        try {
          ps.kill(pid, 0);
          return false;
        } catch {
          fs.unlinkSync(env.lockFile); // it's a craplet
          if (checkStale) return this.instanceLock(false);
        }
      }
    }
    return true;
  }
})();

export function trimLines(s: string): string[] {
  return s.split(/[\n\r\f]+/).filter(x => x.trim());
}

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
