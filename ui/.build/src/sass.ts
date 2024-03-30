import * as cps from 'node:child_process';
import * as fs from 'node:fs';
import * as ps from 'node:process';
import * as path from 'node:path';
import tinycolor from 'tinycolor2';
import { env, colors as c, lines, errorMark } from './main';
import { globArray } from './parse';

// TODO refactor without breaking

type ColorMix = { c1: string; c2?: string; op: string; val: number };
const colorMixMap = new Map<string, ColorMix>();
const themeColorMap = new Map<string, Map<string, tinycolor.Instance>>();
const sassWatch: fs.FSWatcher[] = [];
const importMap = new Map<string, Set<string>>(); // (cssFile, sourcesThatImportIt)
const processed = new Set<string>();
let sassPs: cps.ChildProcessWithoutNullStreams | undefined;
let builder: BuildTimer | undefined;

export function stopSass() {
  sassPs?.kill();
  sassPs = undefined;
  for (const x of sassWatch) x.close();
  sassWatch.length = 0;
  builder?.clear();
  builder = undefined;
  importMap.clear();
  processed.clear();
}

export async function sass(): Promise<void> {
  if (!env.sass) return;

  const sources = (await globArray('./*/css/**/[^_]*.scss', { abs: false })).filter(
    x => !x.includes('/gen/'),
  );
  builder = new BuildTimer(sources);
  await Promise.allSettled([parseThemeColorDefs(), ...[...builder.sources].map(src => parseImports(src))]);
  await buildColorMixes().then(buildColorWrap);

  if (env.watch) {
    for (const dir of [...importMap.keys()].map(path.dirname)) {
      const watcher = fs.watch(dir);
      watcher.on('change', builder.onChanges.bind(builder, dir));
      watcher.on('error', (err: Error) => env.error(err, 'sass'));
      sassWatch.push(watcher);
    }
  }
  if (builder.sources.size) {
    compile([...builder.sources], false);
  } else env.done(0, 'sass');
}

function compile(sources: string[], tellTheWorld = true) {
  if (!sources.length) return;
  if (tellTheWorld) {
    for (const srcFile of sources) {
      env.log(`Building '${c.cyan(srcFile)}'`, { ctx: 'sass' });
    }
  } else env.log(`Building css with ${ps.platform}-${ps.arch}/sass`, { ctx: 'sass' });

  const sassExec = path.join(env.buildDir, 'dart-sass', `${ps.platform}-${ps.arch}`, 'sass');
  const sassArgs = ['--no-error-css', '--stop-on-error', '--no-color', '--quiet', '--quiet-deps'];
  sassPs = cps.spawn(
    sassExec,
    sassArgs.concat(
      env.prod ? ['--style=compressed', '--no-source-map'] : ['--embed-sources'],
      sources.map(
        (src: string) =>
          `${src}:${path.join(
            env.cssDir,
            path.basename(src).replace(/(.*)scss$/, env.prod ? '$1min.css' : '$1dev.css'),
          )}`,
      ),
    ),
  );

  sassPs.stdout?.on('data', (buf: Buffer) => {
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) env.log(c.red(txt), { ctx: 'sass' });
  });
  sassPs.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
  sassPs.on('close', (code: number) => env.done(code, 'sass'));
}

function imports(srcFile: string, bset = new Set<string>()): Set<string> {
  if (bset.has(srcFile)) return bset;
  bset.add(srcFile);
  for (const dep of importMap.get(srcFile) ?? []) imports(dep, bset);
  return bset;
}

async function parseImports(src: string, depth = 1) {
  if (depth > 10) {
    // arbitrary
    env.log(`${errorMark} '${c.cyan(src)}' - max depth exceeded (${depth})`);
    ps.exit(-2);
  }
  if (processed.has(src)) return;
  processed.add(src);
  try {
    const text = await fs.promises.readFile(src, 'utf8');
    if (!path.dirname(src).endsWith('gen')) {
      for (const match of text.matchAll(/\$c_([-_a-z0-9]+)/g)) {
        const [str, mix] = [match[1], parseColor(match[1])];
        if (!mix) {
          env.log(`Invalid color mix: '${c.magenta(str)}' in '${c.cyan(src)}'`, {ctx:'sass'});
          continue;
        }
        colorMixMap.set(str, mix);
      }
    }
    for (const match of text.matchAll(/@(?:import|use)\s+['"](.*)['"]/g)) {
      if (match.length !== 2) continue;
      const absDep = fs.existsSync(path.resolve(path.dirname(src), match[1]) + '.scss')
        ? path.resolve(path.dirname(src), match[1] + '.scss')
        : path.resolve(path.dirname(src), resolvePartial(match[1]));
      if (!absDep.startsWith(env.uiDir) || /node_modules.*\.css/.test(absDep)) continue;
      const dep = absDep.slice(env.uiDir.length + 1);
      if (!importMap.get(dep)?.add(src)) importMap.set(dep, new Set<string>([src]));
      await parseImports(dep, depth + 1);
    }
  } catch (e) {
    env.log(`${errorMark} failed to read ${src} - ${JSON.stringify(e, undefined, 2)}`);
  }
}

async function parseThemeColorDefs() {
  const themeFiles = await globArray('./common/css/theme/_*.scss', { abs: false });
  const themes: string[] = ['dark'];
  for (const themeFile of themeFiles ?? []) {
    const theme = /_([^_]+)\.scss/.exec(themeFile)?.[1];
    if (!theme) {
      env.log(`Invalid theme filename '${c.cyan(themeFile)}'`, {ctx:'sass'});
      continue;
    }
    const text = fs.readFileSync(themeFile, 'utf8');
    const colorMap = new Map<string, tinycolor.Instance>();
    for (const match of text.matchAll(/\s\$c-([-a-z0-9]+):\s*([^;]+);/g)) {
      colorMap.set(match[1], tinycolor(match[2]));
    }
    if (theme !== 'default') themes.push(theme);
    themeColorMap.set(theme, colorMap);
  }
  for (const theme of themes) {
    const colorDefMap = themeColorMap.get(theme) ?? new Map<string, tinycolor.Instance>();

    for (const [color, colorVal] of themeColorMap.get('default') ?? []) {
      if (!colorDefMap.has(color)) colorDefMap.set(color, colorVal.clone());
    }
  }
}

async function buildColorMixes() {
  const out = fs.createWriteStream(path.join(env.themeDir, 'gen', '_mix.scss'));
  for (const theme of themeColorMap.keys()) {
    out.write(`@mixin ${theme}-mix {\n`);
    const colorMap = themeColorMap.get(theme);
    if (!colorMap) continue;
    for (const [colorMix, mix] of colorMixMap) {
      const c1 = colorMap.get(mix.c1)?.clone() ?? new tinycolor(mix.c1);
      const c2 = (mix.c2 ? colorMap.get(mix.c2) : undefined) ?? new tinycolor(mix.c2);
      const mixed =
        mix.op === 'mix'
          ? tinycolor.mix(c2!, c1, clamp(mix.val, { min: 0, max: 100 }))
          : mix.op === 'lighten'
          ? c1.lighten(clamp(mix.val, { min: 0, max: 100 }))
          : mix.op === 'alpha'
          ? c1.setAlpha(clamp(mix.val / 100, { min: 0, max: 1 }))
          : mix.op === 'fade'
          ? c1.setAlpha(c1.getAlpha() * (1 - clamp(mix.val / 100, { min: 0, max: 1 })))
          : undefined;
      if (!mixed) {
        env.error(`Invalid mix op: '${c.magenta(colorMix)}'`, 'sass');
        continue;
      }
      out.write(`  --c_${colorMix}: ${env.rgb ? mixed.toRgbString() : mixed.toHslString()};\n`);
    }
    out.write('}\n\n');
  }
  out.end();
}

async function buildColorWrap() {
  const cssVars = new Set<string>();
  for (const color of colorMixMap.keys()) cssVars.add(`_${color}`);

  for (const file of await globArray(path.join(env.themeDir, '_*.scss'))) {
    for (const line of (await fs.promises.readFile(file, 'utf8')).split('\n')) {
      if (line.startsWith('//') || !line.includes('--c')) continue;
      cssVars.add(line.split(':')[0].trim().replace('--c', ''));
    }
  }
  const scssWrap = Array.from(cssVars)
    .sort()
    .map(variable => `$c${variable}: var(--c${variable});`)
    .join('\n');

  const wrapFile = path.join(env.themeDir, 'gen', '_wrap.scss');
  await fs.promises.mkdir(path.dirname(wrapFile), { recursive: true });
  if (fs.existsSync(wrapFile)) {
    if ((await fs.promises.readFile(wrapFile, 'utf8')) === scssWrap) return; // dont touch wrap if no changes
  }
  await fs.promises.writeFile(wrapFile, scssWrap);
}

function parseColor(colorMix: string) {
  const [clrs, opval] = colorMix.split('--');
  const [c1, c2] = clrs.split('_');
  const [op, valstr] = opval.split('-');
  const val = parseInt(valstr);
  const validColor = (c: string) => themeColorMap.get('default')?.has(c) || tinycolor(c).isValid();
  return (
    validColor(c1) &&
    (op !== 'mix' || validColor(c2)) &&
    ['mix', 'lighten', 'alpha', 'fade'].includes(op) &&
    val >= 0 &&
    val <= 100
  ) ? { c1, c2, op, val } : undefined;
}

function resolvePartial(partial: string): string {
  const nameBegin = partial.lastIndexOf(path.sep) + 1;
  return `${partial.slice(0, nameBegin)}_${partial.slice(nameBegin)}.scss`;
}

function sassError(error: string) {
  for (const err of lines(error)) {
    if (err.startsWith('Error:')) {
      env.log(c.grey('-'.repeat(75)), { ctx: 'sass' });
      env.log(`${errorMark} - ${err.slice(7)}`, { ctx: 'sass' });
    } else env.log(err, { ctx: 'sass' });
  }
}

function clamp(val: number, { min, max }: { min: number; max: number }) {
  return Math.min(max, Math.max(min, val));
}

class BuildTimer {
  fileSet = new Set<string>();
  touched = new Set<string>();
  timeout: NodeJS.Timeout | undefined;
  sources: Set<string>;

  constructor(sources: string[]) {
    this.sources = new Set<string>(sources);
  }

  clear() {
    clearTimeout(this.timeout);
    this.timeout = undefined;
    this.fileSet.clear();
    this.touched.clear();
  }

  add(files: string[]): boolean {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => this.fire(), 200);
    if (files.every(f => this.touched.has(f))) return false;
    files.forEach(src => {
      this.touched.add(src);
      imports(src).forEach(dest => this.fileSet.add(dest));
    });
    return true;
  }

  onChanges = (dir: string, eventType: string, srcFile: string) => {
    if (eventType === 'change') {
      if (this.add([path.join(dir, srcFile)])) env.log(`File '${c.cyanBold(srcFile)}' changed`);
    } else if (eventType === 'rename') {
      globArray('*.scss', { cwd: dir, abs: false }).then(files => {
        if (this.add(files.map(f => path.join(dir, f)))) {
          env.log(`Directory '${c.cyanBold(dir)}' changed`, { ctx: 'sass' });
        }
      });
    }
  };

  async fire() {
    const sources = [...this.fileSet].filter(x => this.sources.has(x));
    const touched = [...this.touched];
    this.clear();

    let rebuildColors = false;

    for (const src of touched) {
      processed.delete(src);
      if (src.includes('common/css/theme/_')) {
        rebuildColors = true;
        await parseThemeColorDefs();
      }
    }
    const oldMixSet = new Set([...colorMixMap.keys()]);
    for (const src of touched) await parseImports(src);
    const newMixSet = new Set([...colorMixMap.keys()]);
    if (oldMixSet.size !== newMixSet.size) rebuildColors = true;
    else if (!rebuildColors) {
      for (const mix of newMixSet) {
        if (!oldMixSet.has(mix)) {
          rebuildColors = true;
          break;
        }
      }
    }
    if (rebuildColors) {
      buildColorMixes()
        .then(buildColorWrap)
        .then(() => compile(sources));
    } else compile(sources);
  }
}
