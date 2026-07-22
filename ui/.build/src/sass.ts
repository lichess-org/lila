import cps from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import { basename, dirname, join, relative, resolve } from 'node:path';
import ps from 'node:process';

import { c, env, errorMark, trimLines } from './env.ts';
import { hashedBasename, symlinkTargetHashes } from './hash.ts';
import { updateManifest } from './manifest.ts';
import { glob, readable } from './parse.ts';
import { makeTask, runTask } from './task.ts';

const importMap = new Map<string, Set<string>>();

let sassPs: cps.ChildProcessWithoutNullStreams | undefined;

export function stopSass(): void {
  sassPs?.removeAllListeners();
  sassPs?.kill();
  sassPs = undefined;
  importMap.clear();
}

export async function sass(): Promise<string | undefined> {
  if (!env.begin('sass')) return;

  await Promise.allSettled([
    fs.promises.mkdir(env.cssOutDir),
    fs.promises.mkdir(env.themeGenDir),
    fs.promises.mkdir(join(env.buildTempDir, 'css')),
  ]);

  let remaining: Set<string> | undefined;

  makeTask({
    // this one just tickles the main sass task when scss variable mappings change
    includes: { cwd: env.themeGenDir, path: '_wrap.scss' },
    debounce: 500,
    monitorOnly: true,
    execute: () => {
      remaining = undefined;
      return runTask('sass');
    },
  });
  return makeTask({
    ctx: 'sass',
    key: 'sass',
    includes: [
      { cwd: env.uiDir, path: '*/css/**/*.scss' },
      { cwd: env.hashOutDir, path: '*' },
    ],
    excludes: '**/gen/**.scss',
    debounce: 300,
    root: env.rootDir,
    execute: async (modified, fullList) => {
      const concreteAll = new Set(fullList.filter(isConcrete));
      const partialTouched = modified.filter(isPartial);
      const urlTargetTouched = await sourcesWithUrls(modified.filter(isUrlTarget));
      const transitiveTouched = [...partialTouched, ...urlTargetTouched].flatMap(p => [...dependsOn(p)]);
      const concreteTouched = [...new Set([...transitiveTouched, ...modified])].filter(isConcrete);
      const themesTouched = partialTouched.some(src => src.startsWith('ui/lib/css/theme/_theme.'));
      remaining = remaining
        ? new Set([...remaining, ...concreteTouched].filter(x => concreteAll.has(x)))
        : concreteAll;
      const processed = new Set<string>();
      await Promise.all(concreteTouched.map(src => parseScss(src, processed)));

      if (themesTouched) {
        await buildColorWrap();
        for (const src of await glob('lib.theme.*.scss', { cwd: 'ui/lib/css/build' }))
          remaining.add(relative(env.rootDir, src));
      }
      const buildSources = [...remaining];
      remaining = new Set(await compile(buildSources, remaining.size < concreteAll.size));

      if (remaining.size) throw `in ${[...remaining].map(s => `'${c.cyan(s)}'`).join(', ')}`;
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
        Promise.all(sources.map(async s => ({ s, exists: await readable(absTempCss(s)) })))
          .then(srcExists => resolveWithErrors(srcExists.filter(({ exists }) => !exists).map(({ s }) => s)))
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

// create scss variables for all css color variables as $c-color: var(--c-color) in _wrap.scss
async function buildColorWrap() {
  const cssVars = new Set<string>();

  for (const file of await glob(join(env.themeDir, '_*.scss'))) {
    if (!file.includes('theme.')) continue;
    for (const line of (await fs.promises.readFile(file, 'utf8')).split('\n')) {
      if (!line.includes('--c-')) continue;
      const commentIndex = line.indexOf('//');
      if (commentIndex !== -1 && commentIndex < line.indexOf(':')) continue;
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
    if ((await fs.promises.readFile(wrapFile, 'utf8')) === scssWrap) return; // dont touch wrap if same
  }
  return fs.promises.writeFile(wrapFile, scssWrap);
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

const absTempCss = (scss: string): string => join(env.cssTempDir, `${basename(scss, '.scss')}.css`);

const isConcrete = (src: string): boolean => src.startsWith('ui/') && !basename(src).startsWith('_');

const isPartial = (src: string): boolean => src.startsWith('ui/') && basename(src).startsWith('_');

const isUrlTarget = (src: string): boolean => src.startsWith('public/');
