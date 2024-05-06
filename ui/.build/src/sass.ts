import * as cps from 'node:child_process';
import * as fs from 'node:fs';
import * as ps from 'node:process';
import * as path from 'node:path';
import clr from 'tinycolor2';
import { env, colors as c, lines, errorMark } from './main';
import { globArray } from './parse';
import { css as cssManifest } from './manifest';

const colorMixMap = new Map<string, { c1: string; c2?: string; op: string; val: number }>();
const themeColorMap = new Map<string, Map<string, clr.Instance>>();
const sassWatch: fs.FSWatcher[] = [];
const importMap = new Map<string, Set<string>>(); // (cssFile, sourcesThatImportIt)
const processed = new Set<string>();
let sassPs: cps.ChildProcessWithoutNullStreams | undefined;
let builder: BuildTimer | undefined;

export function stopSass() {
  sassPs?.removeAllListeners();
  sassPs?.kill();
  sassPs = undefined;
  for (const x of sassWatch) x.close();
  sassWatch.length = 0;
  builder?.clear();
  builder = undefined;
  importMap.clear();
  processed.clear();
  colorMixMap.clear();
  themeColorMap.clear();
}

export async function sass(): Promise<void> {
  if (!env.sass) return;

  const sources = await allSources();
  builder = new BuildTimer();
  await Promise.allSettled([parseThemeColorDefs(), ...sources.map(src => parseScss(src))]);
  await buildColorMixes().then(buildColorWrap);

  if (env.watch) {
    for (const dir of [...importMap.keys()].map(path.dirname)) {
      const watcher = fs.watch(dir);
      watcher.on('change', builder.onChanges.bind(builder, dir));
      watcher.on('error', (err: Error) => env.error(err, 'sass'));
      sassWatch.push(watcher);
    }
  }
  compile(sources, false);
  if (!sources.length) env.done(0, 'sass');
}

export async function allSources() {
  return [
    ...new Set((await globArray('./*/css/**/[^_]*.scss', { abs: false })).filter(x => !x.includes('/gen/'))),
  ];
}

// recursively parse scss file and its imports to build dependency and color maps
async function parseScss(src: string) {
  if (path.dirname(src).endsWith('/gen')) return;
  if (processed.has(src)) return;
  processed.add(src);
  try {
    const text = await fs.promises.readFile(src, 'utf8');
    for (const match of text.matchAll(/\$m-([-_a-z0-9]+)/g)) {
      const [str, mix] = [match[1], parseColor(match[1])];
      if (!mix) {
        env.log(`${errorMark} - invalid color mix: '${c.magenta(str)}' in '${c.cyan(src)}'`, {
          ctx: 'sass',
        });
        continue;
      }
      colorMixMap.set(str, mix);
    }
    for (const match of text.matchAll(/@(?:import|use)\s+['"](.*)['"]/g)) {
      if (match.length !== 2) continue;

      const absDep = fs.existsSync(path.resolve(path.dirname(src), match[1]) + '.scss')
        ? path.resolve(path.dirname(src), match[1] + '.scss')
        : path.resolve(path.dirname(src), resolvePartial(match[1]));

      if (!absDep.startsWith(env.uiDir) || /node_modules.*\.css/.test(absDep)) continue;

      const dep = absDep.slice(env.uiDir.length + 1);
      if (!importMap.get(dep)?.add(src)) importMap.set(dep, new Set<string>([src]));
      await parseScss(dep);
    }
  } catch (e) {
    env.log(`${errorMark} failed to read ${src} - ${JSON.stringify(e, undefined, 2)}`);
  }
}

// collect mixable scss color definitions from theme files
async function parseThemeColorDefs() {
  const themeFiles = await globArray('./common/css/theme/_*.scss', { abs: false });
  const themes: string[] = ['dark'];
  for (const themeFile of themeFiles ?? []) {
    const theme = /_([^/]+)\.scss/.exec(themeFile)?.[1];
    if (!theme) {
      env.log(`${errorMark} - invalid theme filename '${c.cyan(themeFile)}'`, { ctx: 'sass' });
      continue;
    }
    const text = fs.readFileSync(themeFile, 'utf8');
    const colorMap = new Map<string, clr.Instance>();
    for (const match of text.matchAll(/\s\$c-([-a-z0-9]+):\s*([^;]+);/g)) {
      colorMap.set(match[1], clr(match[2]));
    }
    if (theme !== 'default') themes.push(theme);
    themeColorMap.set(theme, colorMap);
  }
  for (const theme of themes) {
    const colorDefMap = themeColorMap.get(theme) ?? new Map<string, clr.Instance>();

    for (const [color, colorVal] of themeColorMap.get('default') ?? []) {
      if (!colorDefMap.has(color)) colorDefMap.set(color, colorVal.clone());
    }
  }
}

// given color definitions and mix instructions, build mixed color css variables in themed scss mixins
async function buildColorMixes() {
  const out = fs.createWriteStream(path.join(env.themeGenDir, '_mix.scss'));
  for (const theme of themeColorMap.keys()) {
    const colorMap = themeColorMap.get(theme)!;
    out.write(`@mixin ${theme}-mix {\n`);
    const colors: string[] = [];
    for (const [colorMix, mix] of colorMixMap) {
      const c1 = colorMap.get(mix.c1)?.clone() ?? new clr(mix.c1);
      const c2 = (mix.c2 ? colorMap.get(mix.c2) : undefined) ?? new clr(mix.c2);
      const mixed =
        mix.op === 'mix'
          ? clr.mix(c2!, c1, clamp(mix.val, { min: 0, max: 100 }))
          : mix.op === 'lighten'
          ? c1.lighten(clamp(mix.val, { min: 0, max: 100 }))
          : mix.op === 'alpha'
          ? c1.setAlpha(clamp(mix.val / 100, { min: 0, max: 1 }))
          : mix.op === 'fade'
          ? c1.setAlpha(c1.getAlpha() * (1 - clamp(mix.val / 100, { min: 0, max: 1 })))
          : undefined;
      if (mixed) colors.push(`  --m-${colorMix}: ${env.rgb ? mixed.toRgbString() : mixed.toHslString()};`);
      else env.log(`${errorMark} - invalid mix op: '${c.magenta(colorMix)}'`, { ctx: 'sass' });
    }
    out.write(colors.sort().join('\n') + '\n}\n\n');
  }
  out.end();
}

// create scss variables for all css color variables as $c-color: var(--c-color) in _wrap.scss
async function buildColorWrap() {
  const cssVars = new Set<string>();
  for (const color of colorMixMap.keys()) cssVars.add(`m-${color}`);

  for (const file of await globArray(path.join(env.themeDir, '_*.scss'))) {
    for (const line of (await fs.promises.readFile(file, 'utf8')).split('\n')) {
      if (line.startsWith('//') || !/--[cm]/.test(line)) continue;
      cssVars.add(line.split(':')[0].trim().replace('--', ''));
    }
  }
  const scssWrap =
    [...new Set(Array.from(cssVars))]
      .sort()
      .map(variable => `$${variable}: var(--${variable});`)
      .join('\n') + '\n';

  const wrapFile = path.join(env.themeDir, 'gen', '_wrap.scss');
  await fs.promises.mkdir(path.dirname(wrapFile), { recursive: true });
  if (fs.existsSync(wrapFile)) {
    if ((await fs.promises.readFile(wrapFile, 'utf8')) === scssWrap) return; // don't touch wrap if no changes
  }
  await fs.promises.writeFile(wrapFile, scssWrap);
}

// compile an array of concrete scss files to ui/.build/dist/css/*.css (css temp dir prior to hashMove)
function compile(sources: string[], tellTheWorld = true) {
  if (!sources.length) return;
  if (tellTheWorld) {
    for (const srcFile of sources) {
      env.log(`Building '${c.cyan(srcFile)}'`, { ctx: 'sass' });
    }
  } else env.log(`Building css with ${ps.platform}-${ps.arch}/sass`, { ctx: 'sass' });

  const sassExec = path.join(env.buildDir, 'dart-sass', `${ps.platform}-${ps.arch}`, 'sass');
  const sassArgs = ['--no-error-css', '--stop-on-error', '--no-color', '--quiet', '--quiet-deps'];
  sassPs?.removeAllListeners();
  sassPs = cps.spawn(
    sassExec,
    sassArgs.concat(
      env.prod ? ['--style=compressed', '--no-source-map'] : ['--embed-sources'],
      sources.map(
        (src: string) =>
          `${src}:${path.join(env.cssTempDir, path.basename(src).replace(/(.*)scss$/, '$1css'))}`,
      ),
    ),
  );

  sassPs.stdout?.on('data', (buf: Buffer) => {
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) env.log(c.red(txt), { ctx: 'sass' });
  });
  sassPs.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
  sassPs.on('close', (code: number) => {
    if (code === 0) cssManifest();
    env.done(code, 'sass');
  });
}

function parseColor(colorMix: string) {
  const [clrs, opval] = colorMix.split('--');
  const [c1, c2] = clrs.split('_');
  const [op, valstr] = opval.split('-');
  const val = parseInt(valstr);
  const validColor = (c: string) => themeColorMap.get('default')?.has(c) || clr(c).isValid();
  return validColor(c1) &&
    (op !== 'mix' || validColor(c2)) &&
    ['mix', 'lighten', 'alpha', 'fade'].includes(op) &&
    val >= 0 &&
    val <= 100
    ? { c1, c2, op, val }
    : undefined;
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

function importersOf(srcFile: string, bset = new Set<string>()): Set<string> {
  if (bset.has(srcFile)) return bset;
  bset.add(srcFile);
  for (const dep of importMap.get(srcFile) ?? []) importersOf(dep, bset);
  return bset;
}

class BuildTimer {
  dependencies = new Set<string>();
  touched = new Set<string>();
  timeout: NodeJS.Timeout | undefined;

  clear() {
    clearTimeout(this.timeout);
    this.timeout = undefined;
    this.dependencies.clear();
    this.touched.clear();
  }

  add(files: string[]): boolean {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => this.fire(), 200);
    if (files.every(f => this.touched.has(f))) return false;
    files.forEach(src => {
      this.touched.add(src);
      importersOf(src).forEach(dest => this.dependencies.add(dest));
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
    const sources = [...this.dependencies].filter(src => /\/[^_][^/]+\.scss$/.test(src));
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
    for (const src of touched) await parseScss(src);
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
