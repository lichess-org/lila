import * as path from 'node:path';
import * as ps from 'node:process';

export interface BleepOpts {
  defaultArgs?: string[]; // arguments when none are given
  gulp?: boolean; // build css, default = true
  log?: {
    heap?: boolean; // show node rss in log statements, default = false
    time?: boolean; // show time in log statements, default = true
    ctx?: boolean; // show context (tsc, rollup, etc), default = true
    color?: any; // set false to disable colors, leave undefined for default, or assign
    // an object that maps contexts -> colors.
    // default = { bleep: 'green', rollup: 'cyan', tsc: 'yellow', gulp: 'magenta' }
  };
}

export interface LichessModule {
  root: string; // absolute path to package.json parentdir (module root)
  name: string; // dirname of module root - usually the module import name
  moduleAlias?: string; // import name (if different from root name as with analysisBoard)
  pkg: any; // the entire package.json object
  build: { pre: string[][]; post: string[][] }; // pre & post build steps from package.json
  hasTsconfig?: boolean; // fileExistsSync('tsconfig.json')
  tscOptions?: string[]; // options from tsc/compile script in package json
  rollup?: LichessRollup[]; // targets from rollup.config.mjs
}

export interface LichessRollup {
  hostMod: LichessModule;
  input: string; // abs path to source
  output: string; // abs path to bundle destination
  importName?: string; // might as well be isAnalysisBoard boolean
  plugins?: any[]; // currently just to copy/replace stuff in site bundle
  onWarn?: (w: any, wf: any) => any; // to suppress 'this is undefined'
  isMain: boolean; // false for plugin bundles
}

export function init(root: string, opts?: BleepOpts) {
  env.rootDir = root;
  env.opts = opts ? opts : { log: {} };
  if (env.opts.log && env.opts.log.color === undefined) {
    env.opts.log.color = {
      bleep: 'green',
      gulp: 'magenta',
      tsc: 'yellow',
      rollup: 'cyan',
    };
  }
}

const lines = (s: string): string[] => s.split(/[\n\r\f]+/).filter(x => x.trim());

const colorLines = (text: string, code: string) =>
  lines(text)
    .map(t => escape(t, code))
    .join('\n');

export const colorFuncs = {
  red: (text: string): string => colorLines(text, codes.red),
  green: (text: string): string => colorLines(text, codes.green),
  yellow: (text: string): string => colorLines(text, codes.yellow),
  blue: (text: string): string => colorLines(text, codes.blue),
  magenta: (text: string): string => colorLines(text, codes.magenta),
  cyan: (text: string): string => colorLines(text, codes.cyan),
  grey: (text: string): string => colorLines(text, codes.grey),
  black: (text: string): string => colorLines(text, codes.black),
};

class Env {
  rootDir: string; // absolute path to lila project root
  opts: BleepOpts; // configure logging mostly

  get uiDir(): string {
    return path.resolve(this.rootDir, 'ui');
  }
  get nodeDir(): string {
    return path.resolve(this.rootDir, 'node_modules');
  }
  get outDir(): string {
    return path.resolve(this.rootDir, 'public/compiled');
  }
  get bleepDir(): string {
    return path.resolve(this.rootDir, 'ui/@build/bleep');
  }
  get tsconfigDir(): string {
    return path.resolve(this.rootDir, 'ui/@build/bleep/.tsconfig');
  }

  log(d: any, { ctx = 'bleep', error = false } = {}) {
    let text: string =
      typeof d == 'string'
        ? d
        : d instanceof Buffer
        ? d.toString('utf8')
        : Array.isArray(d)
        ? d.join('\n')
        : JSON.stringify(d, undefined, 2);

    const show = this.opts.log;
    const esc = show?.color ? escape : (text: string, _: any) => text;
    const rss = Math.round(ps.memoryUsage.rss() / (1000 * 1000));

    if (!show?.color) text = stripColorEscapes(text);

    // strip the time displays from these contexts for consistent formatting
    if (ctx == 'gulp') text = text.replace(/\[\d\d:\d\d:\d\d] /, '');
    else if (ctx == 'tsc') text = text.replace(/\d?\d:\d\d:\d\d (PM|AM) /, '').replace('- ', '');

    const prefix = (
      (show?.time === false ? '' : prettyTime()) +
      (!ctx || show?.ctx === false ? '' : `[${hasColor(ctx) ? ctx : esc(ctx, colorForCtx(ctx, show?.color))}] `) +
      (show?.heap === true ? `${esc(rss + ' MB', rss > 5000 ? codes.red : codes.grey)} ` : '')
    ).trim();
    lines(text).forEach(line => console.log(`${prefix ? prefix + ' - ' : ''}${error ? esc(line, '31') : line}`));
  }
}

export const env = new Env();

export const codes: any = {
  black: '30',
  red: '31',
  green: '32',
  yellow: '33',
  blue: '34',
  magenta: '35',
  cyan: '36',
  grey: '90',
};

const colorForCtx = (ctx: string, color: any): string =>
  color && ctx in color && color[ctx] in codes ? codes[color[ctx]] : codes.grey;

const escape = (text: string, code: string): string => `\x1b[${code}m${stripColorEscapes(text)}\x1b[0m`;

function hasColor(text: string): boolean {
  // eslint-disable-next-line no-control-regex
  return text.match(/\x1b\[[0-9;]*m/) != null;
}
const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`);

function stripColorEscapes(text: string) {
  // eslint-disable-next-line no-control-regex
  return text.replace(/\x1b\[[0-9;]*m/, '');
}

function prettyTime() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())} `;
}
