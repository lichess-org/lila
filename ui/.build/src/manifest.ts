import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as fs from 'node:fs';
import * as crypto from 'node:crypto';
import * as es from 'esbuild';
import { env, colors as c } from './main';
import { globArray } from './parse';
import { allSources } from './sass';

type Manifest = { [key: string]: { hash?: string; imports?: string[] } };

const current: { js: Manifest; css: Manifest } = { js: {}, css: {} };
let writeTimer: NodeJS.Timeout;

export async function init() {
  if (env.building.length === env.modules.size) return;
  // we're building a subset of modules. if possible reuse the previously built full manifest to give us
  // a shot at changes viewable in the browser, otherwise punt.
  const existing = path.join(env.jsDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`);
  if (!fs.existsSync(existing)) return;
  if (Object.keys(current.js).length && Object.keys(current.css).length) return;
  const manifest = JSON.parse(await fs.promises.readFile(existing, 'utf-8'));
  delete manifest.js.manifest;
  current.js = manifest.js;
  current.css = manifest.css;
}

export async function css() {
  const files = await globArray(path.join(env.cssTempDir, '*.css'), { abs: true });
  const css: { name: string; hash: string }[] = await Promise.all(files.map(hashMove));
  const newCssManifest: Manifest = {};
  for (const { name, hash } of css) newCssManifest[name] = { hash };
  if (isEquivalent(newCssManifest, current.css)) return;
  current.css = shallowSort({ ...current.css, ...newCssManifest });
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

export async function js(meta: es.Metafile) {
  const newJsManifest: Manifest = {};
  for (const [filename, info] of Object.entries(meta.outputs)) {
    const out = parsePath(filename);
    if (!out) continue;
    if (out.name === 'common') {
      out.name = `common.${out.hash}`;
      newJsManifest[out.name] = {};
    } else newJsManifest[out.name] = { hash: out.hash };
    const imports: string[] = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = parsePath(imp.path);
        if (path) imports.push(`${path.name}.${path.hash}.js`);
      }
    }
    newJsManifest[out.name].imports = imports;
  }
  if (isEquivalent(newJsManifest, current.js)) return;
  current.js = shallowSort({ ...current.js, ...newJsManifest });
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

async function write() {
  if (!env.manifestOk || !(await isComplete())) return;
  const commitMessage = cps
    .execSync('git log -1 --pretty=%s', { encoding: 'utf-8' })
    .trim()
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;');
  const clientJs: string[] = [
    'if (!window.site) window.site={};',
    'if (!window.site.info) window.site.info={};',
    `window.site.info.commit='${cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim()}';`,
    `window.site.info.message='${commitMessage}';`,
    `window.site.debug=${env.debug};`,
    'const m=window.site.manifest={css:{},js:{}};',
  ];
  for (const [name, info] of Object.entries(current.js)) {
    if (!/common\.[A-Z0-9]{8}/.test(name)) clientJs.push(`m.js['${name}']='${info.hash}';`);
  }
  for (const [name, info] of Object.entries(current.css)) {
    clientJs.push(`m.css['${name}']='${info.hash}';`);
  }

  const hashable = clientJs.join('\n');
  const hash = crypto.createHash('sha256').update(hashable).digest('hex').slice(0, 8);
  // add the date after hashing
  const clientManifest =
    hashable +
    `\nwindow.site.info.date='${
      new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00'
    }';\n`;
  const serverManifest = { js: { manifest: { hash }, ...current.js }, css: { ...current.css } };

  await Promise.all([
    fs.promises.writeFile(path.join(env.jsDir, `manifest.${hash}.js`), clientManifest),
    fs.promises.writeFile(
      path.join(env.jsDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`),
      JSON.stringify(serverManifest, null, env.prod ? undefined : 2),
    ),
  ]);
  env.log(`Manifest hash ${c.green(hash)}`);
}

async function hashMove(src: string) {
  const content = await fs.promises.readFile(src, 'utf-8');
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 8);
  const basename = path.basename(src, '.css');
  await Promise.allSettled([
    env.prod ? undefined : fs.promises.rename(`${src}.map`, path.join(env.cssDir, `${basename}.css.map`)),
    fs.promises.rename(src, path.join(env.cssDir, `${basename}.${hash}.css`)),
  ]);
  return { name: path.basename(src, '.css'), hash };
}

function shallowSort(obj: { [key: string]: any }): { [key: string]: any } {
  // es6 string properties are insertion order, we need more determinism
  const sorted: { [key: string]: any } = {};
  for (const key of Object.keys(obj).sort()) sorted[key] = obj[key];
  return sorted;
}

function parsePath(path: string) {
  const match = path.match(/\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/);
  return match ? { name: match[1], hash: match[2] } : undefined;
}

function isEquivalent(a: any, b: any) {
  // key order does NOT matter
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) return a.length === b.length && a.every(x => b.includes(x));
  if (typeof a !== 'object') return a === b;
  for (const key in a) {
    if (!(key in b) || !isEquivalent(a[key], b[key])) return false;
  }
  return true;
}

async function isComplete() {
  for (const bundle of [...env.modules.values()].map(x => x.bundles ?? []).flat()) {
    const name = path.basename(bundle, '.ts');
    if (!current.js[name]) {
      env.log(`Missing entry point '${c.cyan(name)}'. No manifest written.`);
      return false;
    }
  }
  for (const css of await allSources()) {
    const name = path.basename(css, '.scss');
    if (!current.css[name]) {
      env.log(`Missing css '${c.cyan(name)}'. No manifest written.`);
      return false;
    }
  }
  return true;
}
