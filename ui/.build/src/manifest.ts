import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as fs from 'node:fs';
import * as crypto from 'node:crypto';
import * as es from 'esbuild';
import { env } from './main';
import { globArray } from './parse';

type Manifest = { [key: string]: { hash?: string; imports?: string[] } };
const current: { js: Manifest; css: Manifest } = { js: {}, css: {} };
let writeTimer: NodeJS.Timeout;

export async function css() {
  const files = await globArray(path.join(env.cssTempDir, '*.css'), { abs: true });
  const css: string[][] = await Promise.all(files.map(hashMove));
  const newCssManifest: Manifest = {};
  for (const [name, hash] of css) newCssManifest[name] = { hash };
  if (equivalent(newCssManifest, current.css)) return;
  current.css = shallowSort({ ...current.css, ...newCssManifest });
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

const jsPathRe = /\.\.\/\.\.\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/;
export async function js(meta: es.Metafile) {
  const newJsManifest: Manifest = {};
  for (const [filename, info] of Object.entries(meta.outputs)) {
    const match = filename.match(jsPathRe);
    if (!match || !match[1] || !match[2]) continue;
    let [name, hash] = [match[1], match[2]];
    if (name === 'common') {
      name = `common.${hash}`;
      newJsManifest[name] = {};
    } else newJsManifest[name] = { hash };
    const imports: string[] = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = parsePath(imp.path);
        if (path) imports.push(`${path.name}.${path.hash}.js`);
      }
    }
    newJsManifest[name].imports = imports;
  }
  if (equivalent(newJsManifest, current.js)) return;
  current.js = shallowSort({ ...current.js, ...newJsManifest });
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

async function write() {
  const commitMessage = cps
    .execSync('git log -1 --pretty=%s', { encoding: 'utf-8' })
    .trim()
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;');
  const clientJs: string[] = [
    'window.site??={};',
    'window.site.info??={};',
    `window.site.info.date='${new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00'}';`,
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

  const clientManifest = clientJs.join('\n');
  const hash = crypto.createHash('sha256').update(clientManifest).digest('hex').slice(0, 8);
  const serverManifest = { js: { manifest: { hash }, ...current.js }, css: { ...current.css } };

  await Promise.all([
    fs.promises.writeFile(path.join(env.jsDir, `manifest.${hash}.js`), clientManifest),
    fs.promises.writeFile(
      path.join(env.confDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`),
      JSON.stringify(serverManifest, null, env.prod ? undefined : 2),
    ),
  ]);
}

async function hashMove(src: string) {
  const content = await fs.promises.readFile(src, 'utf-8');
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 8);
  const basename = path.basename(src, '.css');
  await Promise.allSettled([
    env.prod ? undefined : fs.promises.rename(`${src}.map`, path.join(env.cssDir, `${basename}.css.map`)),
    fs.promises.rename(src, path.join(env.cssDir, `${basename}.${hash}.css`)),
  ]);
  return [path.basename(src, '.css'), hash];
}

function shallowSort(obj: { [key: string]: any }): { [key: string]: any } {
  // es6 string properties are insertion order, we need more determinism
  const sorted: { [key: string]: any } = {};
  for (const key of Object.keys(obj).sort()) sorted[key] = obj[key];
  return sorted;
}

function parsePath(path: string) {
  const match = path.match(jsPathRe);
  return match ? { name: match[1], hash: match[2] } : undefined;
}

function equivalent(a: any, b: any) {
  // key order does NOT matter
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) return a.length === b.length && a.every(x => b.includes(x));
  if (typeof a !== 'object') return a === b;
  for (const key in a) {
    if (!(key in b) || !equivalent(a[key], b[key])) return false;
  }
  return true;
}
