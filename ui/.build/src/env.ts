import fs from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import ps from 'node:process';
import pc from 'picocolors';

import { definedUnique, isEquivalent } from './algo.ts';

export interface Package {
  root: string; // absolute path to package.json parentdir
  name: string; // dirname of package root
  pkg: any; // package.json object
  bundle: Bundle[]; // esbuild bundling
  hash: Hash[]; // files to symlink hash
  sync: Sync[]; // pre-bundle filesystem copies from package json
}

export interface Hash {
  path: string; // glob for assets
  catalog?: string; // file to update with hashed filenames
  omit?: boolean; // omit from client manifest, default false
}

export interface Bundle {
  module?: string; // file glob for esm modules (esbuild entry points)
  inline?: string; // inject this script into response html
}

export interface Sync {
  src: string; // file glob expression, use <dir>/** to sync entire directories
  dest: string; // directory to copy into
}

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
  remoteLog: string | boolean = false;
  startTime?: number;

  packages: Map<string, Package> = new Map();
  workspaceDeps: Map<string, string[]> = new Map();
  building: Package[] = [];
  mustSucceed = new Set<() => boolean>();
  onSuccess = new Set<() => void>();
  private readonly contextStartedAt = new Map<Context, number>();

  readonly status = {} as Record<Context, number | false | undefined>;

  *tasks<T extends 'sync' | 'hash' | 'bundle'>(
    t: T,
  ): Generator<[Package, Package[T] extends Array<infer U> ? U : never]> {
    for (const pkg of this.building) {
      for (const item of pkg[t] as (Package[T] extends (infer U)[] ? U : never)[]) {
        yield [pkg, item];
      }
    }
  }

  buildOk(): boolean {
    return (
      [...this.mustSucceed].every(isOk => isOk()) &&
      isEquivalent(this.building, [...this.packages.values()]) &&
      (['tsc', 'esbuild', 'sass', 'i18n'] as const).map(b => this.status[b]).every(x => x === 0)
    );
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
      (this.logTime ? `${pc.gray(prettyTime())} ` : '') + (ctx && this.logCtx ? colorForCtx(ctx)(ctx) : '')
    ).trim();
    const sep = prefix ? ` ${pc.dim('⏵')} ` : '';
    for (const line of trimLines(text)) {
      console.log(maybeStripEscapes(`${prefix}${sep}${line}`));
    }
  }

  exit(d?: any, ctx = 'build'): void {
    if (d) this.log(d, ctx);
    process.exit(1);
  }

  begin(ctx: Context, enable?: boolean): boolean {
    if (enable === false) this.status[ctx] = false;
    else if (enable === true || this.status[ctx] !== false) {
      if (!this.contextStartedAt.has(ctx)) this.contextStartedAt.set(ctx, Date.now());
      this.status[ctx] = undefined;
    }
    return this.status[ctx] !== false;
  }

  setStatus(ctx: Context, code: number | undefined): void {
    if (code !== undefined && code !== this.status[ctx]) {
      const startedAt = this.contextStartedAt.get(ctx);
      const took =
        code === 0 && startedAt ? pc.gray(` (${((Date.now() - startedAt) / 1000).toFixed(3)}s)`) : '';
      this.log(
        `${code === 0 ? `Done${took}` : pc.red('Failed')}${this.watch ? ` ${pc.green('• Watching…')}` : ''}`,
        ctx,
      );
      this.contextStartedAt.delete(ctx);
    }
    this.status[ctx] = code;
    if (this.buildOk()) {
      if (this.startTime) {
        const doneMsg = `Done in ${pc.greenBright(String((Date.now() - this.startTime) / 1000) + 's')}`;
        this.log(doneMsg + (this.stdin ? `. Press ${pc.gray('<space>')} to trigger clean rebuild` : ''));
      }
      this.onSuccess.forEach(yay => yay());
      this.startTime = undefined;
    }
    if (!this.watch && code) process.exit(code);
  }

  instanceLock(strategy: 'check' | 'kill' | 'acquire' = 'check'): boolean {
    try {
      const fd = fs.openSync(env.lockFile, 'wx');
      fs.writeFileSync(fd, String(ps.pid), { flag: 'w' });
      fs.closeSync(fd);
      ps.on('exit', () => {
        try {
          if (ps.pid === Number(fs.readFileSync(env.lockFile))) fs.unlinkSync(env.lockFile);
        } catch {}
      });
    } catch {
      const pid = parseInt(fs.readFileSync(env.lockFile, 'utf8'), 10);
      if (!isNaN(pid) && pid > 0 && ps.platform !== 'win32') {
        try {
          ps.kill(pid, strategy === 'kill' ? 'SIGINT' : 0);
          if (strategy === 'check') return false;
        } catch {}
        fs.unlinkSync(env.lockFile); // it's a craplet
        if (strategy !== 'acquire') return this.instanceLock('acquire');
      }
    }
    return true;
  }
})();

export const trimLines = (s: string): string[] => s.split(/[\n\r\f]+/).filter(x => x.trim());

export type Context = 'sass' | 'tsc' | 'esbuild' | 'sync' | 'hash' | 'i18n' | 'web';

const contextColors: Record<string, (text: string) => string> = {
  build: pc.green,
  sass: pc.magenta,
  tsc: pc.yellow,
  esbuild: x => pc.bold(pc.blue(x)),
  sync: pc.cyan,
  hash: pc.blue,
  i18n: x => pc.bold(pc.cyan(x)),
  web: x => pc.bold(pc.magenta(x)),
};

function colorForCtx(ctx: string): (text: string) => string {
  return contextColors[ctx] ?? pc.whiteBright;
}

export const errorMark: string = pc.red('✘ ') + pc.redBright('[ERROR]');
export const warnMark: string = pc.yellow('⚠ ') + pc.yellowBright('[WARNING]');

const timeFormatter = new Intl.DateTimeFormat('en-GB', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
});

function maybeStripEscapes(text: string) {
  return pc.isColorSupported ? text : text.replace(/\x1b\[[0-9;]*m/, '');
}

function prettyTime() {
  return timeFormatter.format(new Date());
}
