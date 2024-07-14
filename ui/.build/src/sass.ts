import * as cps from 'node:child_process';
import * as fs from 'node:fs';
import * as ps from 'node:process';
import * as path from 'node:path';
import clr from 'tinycolor2';
import { env, colors as c, lines, errorMark } from './main';
import { globArray } from './parse';
import { cssManifest } from './manifest';

const colorMixMap = new Map<string, { c1: string; c2?: string; op: string; val: number }>();
const themeColorMap = new Map<string, Map<string, clr.Instance>>();
const importMap = new Map<string, Set<string>>();
const processed = new Set<string>();
let sassPs: cps.ChildProcessWithoutNullStreams | undefined;
let watcher: SassWatch | undefined;
let awaitingFullBuild: boolean | undefined = undefined;

export function stopSass() {
  sassPs?.removeAllListeners();
  sassPs?.kill();
  sassPs = undefined;
  watcher?.destroy();
  watcher = undefined;
  importMap.clear();
  processed.clear();
  colorMixMap.clear();
  themeColorMap.clear();
}

export async function sass(): Promise<void> {
  if (!env.sass) return;

  awaitingFullBuild ??= env.watch && env.building.length === env.modules.size;

  const sources = await allSources();
  await Promise.allSettled([parseThemeColorDefs(), ...sources.map(src => parseScss(src))]);
  await buildColorMixes().then(buildColorWrap);

  if (env.watch) watcher = new SassWatch();

  compile(sources, env.building.length !== env.modules.size);

  if (!sources.length) env.done(0, 'sass');
}

export async function allSources(): Promise<string[]> {
  return (await globArray('./*/css/**/[^_]*.scss', { absolute: false })).filter(x => !x.includes('/gen/'));
}

async function unbuiltSources(): Promise<string[]> {
  return (await allSources()).filter(
    src => !fs.existsSync(path.join(env.cssTempDir, `${path.basename(src, '.scss')}.css`)),
  );
}

// compile an array of concrete scss files to ui/.build/dist/css/*.css (css temp dir prior to hashMove)
function compile(sources: string[], logAll = true) {
  if (!sources.length) return sources.length;

  const sassExec =
    process.env.SASS_PATH || path.join(env.buildDir, 'dart-sass', `${ps.platform}-${ps.arch}`, 'sass');

  if (logAll) sources.forEach(src => env.log(`Building '${c.cyan(src)}'`, { ctx: 'sass' }));
  else env.log(`Building css with ${sassExec}`, { ctx: 'sass' });

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

  sassPs.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
  sassPs.stdout?.on('data', (buf: Buffer) => {
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) env.log(c.red(txt), { ctx: 'sass' });
  });
  sassPs.on('close', async (code: number) => {
    if (code !== 0) return env.done(code, 'sass');
    if (awaitingFullBuild && compile(await unbuiltSources()) > 0) return;
    awaitingFullBuild = false; // now we are ready to make manifests
    cssManifest();
    env.done(0, 'sass');
  });
  return sources.length;
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
    for (const match of text.matchAll(/^@(?:import|use)\s+['"](.*)['"]/gm)) {
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
  const themeFiles = await globArray('./common/css/theme/_*.scss', { absolute: false });
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
      if (line.indexOf('--') === -1) continue;
      const commentIndex = line.indexOf('//');
      if (commentIndex !== -1 && commentIndex < line.indexOf(':')) continue;
      if (!/--[cm]/.test(line)) continue;
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

class SassWatch {
  dependencies = new Set<string>();
  touched = new Set<string>();
  timeout: NodeJS.Timeout | undefined;
  watchers: fs.FSWatcher[] = [];
  watchDirs = new Set<string>();
  constructor() {
    this.watch();
  }

  async watch() {
    if (!env.watch) return;
    const watchDirs = new Set<string>([...importMap.keys()].map(path.dirname));
    (await allSources()).forEach(s => watchDirs.add(path.dirname(s)));
    if (this.watchDirs.size === watchDirs.size || [...watchDirs].every(d => this.watchDirs.has(d))) return;
    if (this.watchDirs.size) env.log('Rebuilding watchers...', { ctx: 'sass' });
    for (const x of this.watchers) x.close();
    this.watchers.length = 0;
    this.watchDirs = watchDirs;
    for (const dir of this.watchDirs) {
      const fsWatcher = fs.watch(dir);
      fsWatcher.on('change', (event: string, srcFile: string) => this.onChange(dir, event, srcFile));
      fsWatcher.on('error', (err: Error) => env.error(err, 'sass'));
      this.watchers.push(fsWatcher);
    }
  }

  destroy() {
    this.clear();
    for (const x of this.watchers) x.close();
    this.watchers.length = 0;
  }

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
      if (!/[^_].*\.scss/.test(path.basename(src))) {
        this.dependencies.add(src);
      } else importersOf(src).forEach(dest => this.dependencies.add(dest));
    });
    return true;
  }

  onChange(dir: string, event: string, srcFile: string) {
    if (event === 'change') {
      if (this.add([path.join(dir, srcFile)])) env.log(`File '${c.cyanBold(srcFile)}' changed`);
    } else if (event === 'rename') {
      globArray('*.scss', { cwd: dir, absolute: false }).then(files => {
        if (this.add(files.map(f => path.join(dir, f)))) {
          env.log(`Cross your fingers - directory '${c.cyanBold(dir)}' changed`, { ctx: 'sass' });
        }
      });
    }
  }

  async fire() {
    const sources = [...this.dependencies].filter(src => /\/[^_][^/]+\.scss$/.test(src));
    const touched = [...this.touched];
    this.clear();
    this.watch(); // experimental
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
    if ([...newMixSet].some(mix => !oldMixSet.has(mix))) rebuildColors = true;
    if (rebuildColors) {
      buildColorMixes()
        .then(buildColorWrap)
        .then(() => compile(sources));
    } else compile(sources);
  }
}
