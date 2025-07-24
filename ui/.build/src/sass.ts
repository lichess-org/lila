import cps from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import { basename, dirname, join, relative, resolve } from 'node:path';
import ps from 'node:process';
import clr from 'tinycolor2';
import { clamp, isEquivalent } from './algo.ts';
import { c, env, errorMark, trimLines } from './env.ts';
import { hashedBasename, symlinkTargetHashes } from './hash.ts';
import { updateManifest } from './manifest.ts';
import { glob, readable } from './parse.ts';
import { task } from './task.ts';

const importMap = new Map<string, Set<string>>();
const colorMixMap = new Map<string, { c1: string; c2?: string; op: string; val: number }>();
const themeColorMap = new Map<string, Map<string, clr.Instance>>();

let sassPs: cps.ChildProcessWithoutNullStreams | undefined;

export function stopSass(): void {
  sassPs?.removeAllListeners();
  sassPs?.kill();
  sassPs = undefined;
  importMap.clear();
  colorMixMap.clear();
  themeColorMap.clear();
}

export async function sass(): Promise<any> {
  if (!env.begin('sass')) return;

  await Promise.allSettled([
    fs.promises.mkdir(env.cssOutDir),
    fs.promises.mkdir(env.themeGenDir),
    fs.promises.mkdir(join(env.buildTempDir, 'css')),
  ]);

  let remaining: Set<string>;

  return task({
    ctx: 'sass',
    includes: [
      { cwd: env.uiDir, path: '*/css/**/*.scss' },
      { cwd: env.hashOutDir, path: '*' },
    ],
    excludes: '**/gen/**',
    debounce: 300,
    root: env.rootDir,
    execute: async (modified, fullList) => {
      const concreteAll = new Set(fullList.filter(isConcrete));
      const partialTouched = modified.filter(isPartial);
      const urlTargetTouched = await sourcesWithUrls(modified.filter(isUrlTarget));
      const transitiveTouched = [...partialTouched, ...urlTargetTouched].flatMap(p => [...dependsOn(p)]);
      const concreteTouched = [...new Set([...transitiveTouched, ...modified])].filter(isConcrete);

      remaining = remaining
        ? new Set([...remaining, ...concreteTouched].filter(x => concreteAll.has(x)))
        : concreteAll;

      if (partialTouched.some(src => src.startsWith('ui/lib/css/theme/_'))) {
        await parseThemeColorDefs();
      }

      const oldMixes = Object.fromEntries(colorMixMap);
      const processed = new Set<string>();
      await Promise.all(concreteTouched.map(src => parseScss(src, processed)));

      if (!isEquivalent(oldMixes, Object.fromEntries(colorMixMap))) {
        await buildColorMixes();
        await buildColorWrap();
        for (const src of await glob('lib.theme.*.scss', { cwd: 'ui/lib/css/build' }))
          remaining.add(relative(env.rootDir, src)); // TODO test me
      }
      const buildSources = [...remaining];
      remaining = new Set(await compile(buildSources, remaining.size < concreteAll.size));

      if (remaining.size) return;

      const replacements = urlReplacements();
      updateManifest({
        css: Object.fromEntries(
          await Promise.all(buildSources.map(async scss => hashCss(absTempCss(scss), replacements[scss]))),
        ),
      });
    },
  });
}

// compile an array of concrete scss files, return any that error
async function compile(sources: string[], logAll = true): Promise<string[]> {
  const sassBin =
    process.env.SASS_PATH ??
    (await fs.promises.realpath(
      join(env.buildDir, 'node_modules', `sass-embedded-${ps.platform}-${ps.arch}`, 'dart-sass', 'sass'),
    ));
  if (!(await readable(sassBin))) env.exit(`Sass executable not found '${c.cyan(sassBin)}'`, 'sass');

  return new Promise(resolveWithErrors => {
    if (!sources.length) return resolveWithErrors([]);
    if (logAll) sources.forEach(src => env.log(`Building '${c.cyan(src)}'`, 'sass'));
    else env.log('Building', 'sass');

    const sassArgs = ['--no-error-css', '--stop-on-error', '--no-color', '--quiet', '--quiet-deps'];
    sassPs?.removeAllListeners();
    sassPs = cps.spawn(
      sassBin,
      sassArgs.concat(
        env.prod ? ['--style=compressed', '--no-source-map'] : ['--embed-sources'],
        sources.map((src: string) => `${src}:${absTempCss(src)}`),
      ),
    );

    sassPs.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
    sassPs.stdout?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
    sassPs.on('close', async (code: number) => {
      sassPs = undefined;
      if (code === 0) resolveWithErrors([]);
      else
        Promise.all(sources.filter(scss => !readable(absTempCss(scss))))
          .then(resolveWithErrors)
          .catch(() => resolveWithErrors(sources));
    });
  });
}

// recursively parse scss file and its imports to build dependency and color maps
async function parseScss(src: string, processed: Set<string>) {
  if (dirname(src).endsWith('/gen')) return;
  if (processed.has(src)) return;
  processed.add(src);

  const text = await fs.promises.readFile(src, 'utf8');

  for (const [, mixName] of text.matchAll(/\$m-([-_a-z0-9]+)/g)) {
    const mixColor = parseColor(mixName);
    if (!mixColor) {
      env.log(`${errorMark} Invalid color mix: '${c.magenta(mixName)}' in '${c.cyan(src)}'`, 'sass');
      continue;
    }
    colorMixMap.set(mixName, mixColor);
  }

  for (const [, urlProp] of text.matchAll(/[^a-zA-Z0-9\-_]url\((?:['"])?(\.\.\/[^'")]+)/g)) {
    const url = urlProp.replaceAll(/#\{[^}]+\}/g, '*'); // scss interpolation -> glob

    if (url.includes('*')) {
      for (const file of await glob(url, { cwd: env.cssOutDir, absolute: false })) {
        if (!importMap.get(file)?.add(src)) importMap.set(file, new Set([src]));
      }
    } else if (!importMap.get(url)?.add(src)) importMap.set(url, new Set([src]));
  }

  for (const [, cssImport] of text.matchAll(/^@(?:import|use)\s+['"](.*)['"]/gm)) {
    if (!cssImport) continue;

    const absDep = (await readable(resolve(dirname(src), cssImport + '.scss')))
      ? resolve(dirname(src), cssImport + '.scss')
      : resolve(dirname(src), resolvePartial(cssImport));

    if (/node_modules.*\.css/.test(absDep)) continue;
    else if (!absDep.startsWith(env.rootDir)) throw `Bad import '${cssImport}`;

    const dep = relative(env.rootDir, absDep);
    if (!importMap.get(dep)?.add(src)) importMap.set(dep, new Set<string>([src]));
    await parseScss(dep, processed);
  }
}

// collect mixable scss color definitions from theme files
async function parseThemeColorDefs() {
  async function loadThemeColors(themeFile: string) {
    const text = await fs.promises.readFile(themeFile, 'utf8');
    const colorMap = new Map<string, clr.Instance>();
    for (const [, color, colorVal] of text.matchAll(/\s\$c-([-a-z0-9]+):\s*([^;]+);/g)) {
      colorMap.set(color, clr(colorVal.trim()));
    }
    return colorMap;
  }
  const themes: string[] = ['dark'];

  const defaultThemeColors = await loadThemeColors(join(env.themeDir, '_default.scss'));
  themeColorMap.set('default', defaultThemeColors);

  const themeFiles = await glob(join(env.themeDir, '_*.scss'), { absolute: false });
  for (const themeFile of themeFiles ?? []) {
    const theme = /_([^/]+)\.scss/.exec(themeFile)?.[1];
    if (!theme) {
      env.log(`${errorMark} Invalid theme filename '${c.cyan(themeFile)}'`, 'sass');
      continue;
    }
    if (theme === 'default') {
      continue;
    }

    themes.push(theme);
    themeColorMap.set(theme, await loadThemeColors(themeFile));
  }

  for (const theme of themes) {
    const colorDefMap = themeColorMap.get(theme) ?? new Map<string, clr.Instance>();

    for (const [color, colorVal] of defaultThemeColors) {
      if (!colorDefMap.has(color)) colorDefMap.set(color, colorVal.clone());
    }
  }
}

// given color definitions and mix instructions, build mixed color css variables in themed scss mixins
async function buildColorMixes() {
  const out = fs.createWriteStream(join(env.themeGenDir, '_mix.scss'));
  for (const theme of themeColorMap.keys()) {
    const colorMap = themeColorMap.get(theme)!;
    out.write(`@mixin ${theme}-mix {\n`);
    const colors: string[] = [];
    for (const [colorMix, mix] of colorMixMap) {
      const c1 = colorMap.get(mix.c1)?.clone() ?? new clr(mix.c1);
      const c2 = (mix.c2 ? colorMap.get(mix.c2) : undefined) ?? new clr(mix.c2);
      const mixed = (() => {
        switch (mix.op) {
          case 'mix':
            return clr.mix(c2!, c1, clamp(mix.val, { min: 0, max: 100 }));
          case 'lighten':
            return c1.lighten(clamp(mix.val, { min: 0, max: 100 }));
          case 'alpha':
            return c1.setAlpha(clamp(mix.val / 100, { min: 0, max: 1 }));
          case 'fade':
            return c1.setAlpha(c1.getAlpha() * (1 - clamp(mix.val / 100, { min: 0, max: 1 })));
        }
      })();
      if (mixed) colors.push(`  --m-${colorMix}: ${mixed.toHslString()};`);
      else env.log(`${errorMark} Invalid mix op: '${c.magenta(colorMix)}'`, 'sass');
    }
    out.write(colors.sort().join('\n') + '\n}\n\n');
  }
  out.end();
}

// create scss variables for all css color variables as $c-color: var(--c-color) in _wrap.scss
async function buildColorWrap() {
  const cssVars = new Set<string>();
  for (const color of colorMixMap.keys()) cssVars.add(`m-${color}`);

  for (const file of await glob(join(env.themeDir, '_*.scss'))) {
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

  const wrapFile = join(env.themeDir, 'gen', '_wrap.scss');
  await fs.promises.mkdir(dirname(wrapFile), { recursive: true });
  if (await readable(wrapFile)) {
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

async function hashCss(src: string, replacements: Record<string, string> | undefined) {
  let content = await fs.promises.readFile(src, 'utf-8');
  let modified = false;

  for (const [search, replace] of Object.entries(replacements ?? {})) {
    content = content.replaceAll(search, replace);
    modified = true;
  }
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 8);
  const baseName = basename(src, '.css');
  const outName = join(env.cssOutDir, `${baseName}.${hash}.css`);
  await Promise.allSettled([
    env.prod ? undefined : fs.promises.rename(`${src}.map`, `${join(env.cssOutDir, baseName)}.css.map`),
    modified
      ? fs.promises.writeFile(outName, content).then(() => fs.promises.unlink(src))
      : fs.promises.rename(src, outName),
  ]);
  return [baseName, { hash }];
}

async function sourcesWithUrls(urls: string[]) {
  const importers = new Set<string>();
  for (const [target, hash] of Object.entries(await symlinkTargetHashes(urls))) {
    const url = relative(env.hashOutDir, join(env.outDir, target));
    const depSet = importMap.get(url) ?? new Set<string>();
    depSet.delete([...depSet].find(f => f.startsWith('public/hashed/')) ?? '');
    depSet.forEach(i => importers.add(i));
    depSet.add(join('public/hashed', hashedBasename(url, hash)));
    importMap.set(url, depSet);
  }
  return [...importers];
}

function urlReplacements() {
  const replacements: Record<string, Record<string, string>> = {};
  for (const [url, depSet] of importMap) {
    if (!url.startsWith('../')) continue;
    const hashed = [...depSet].find(f => f.startsWith('public/hashed/'));
    if (!hashed) continue;
    for (const src of [...dependsOn(url)].filter(isConcrete)) {
      replacements[src] ??= {};
      replacements[src][url] = relative(env.cssOutDir, hashed);
    }
  }
  return replacements;
}

function dependsOn(srcFile: string, bset = new Set<string>()): Set<string> {
  if (srcFile.startsWith('public/hashed/') || bset.has(srcFile)) return bset;
  bset.add(srcFile);
  for (const dep of importMap.get(srcFile) ?? []) dependsOn(dep, bset);
  return bset;
}

function sassError(error: string) {
  for (const err of trimLines(error)) {
    if (err.startsWith('Error:')) {
      env.log(c.grey('-'.repeat(75)), 'sass');
      env.log(`${errorMark} - ${err.slice(7)}`, 'sass');
    } else env.log(err, 'sass');
  }
}

function resolvePartial(partial: string): string {
  const nameBegin = partial.lastIndexOf('/') + 1;
  return `${partial.slice(0, nameBegin)}_${partial.slice(nameBegin)}.scss`;
}

function absTempCss(scss: string) {
  return join(env.cssTempDir, `${basename(scss, '.scss')}.css`);
}

function isConcrete(src: string) {
  return src.startsWith('ui/') && !basename(src).startsWith('_');
}

function isPartial(src: string) {
  return src.startsWith('ui/') && basename(src).startsWith('_');
}

function isUrlTarget(src: string) {
  return src.startsWith('public/');
}
