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
  current.css = { ...current.css, ...newCssManifest };
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

export async function js(meta: es.Metafile) {
  const newJsManifest: Manifest = {};
  for (const [file, info] of Object.entries(meta.outputs)) {
    const out = parsePath(file);
    if (!out) continue;
    let { type, name, hash } = out;
    if (type !== 'js') continue;
    if (name === 'chunk') {
      name = `chunk-${hash}`;
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
  current.js = { ...current.js, ...newJsManifest };
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

async function write() {
  const clientJs: string[] = [
    'window.site??={};',
    'window.site.info??={};',
    `window.site.info.date='${new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00'}';`,
    `window.site.info.commit='${cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim()}';`,
    `window.site.info.message='${cps.execSync('git log -1 --pretty=%s', { encoding: 'utf-8' }).trim()}';`,
    `window.site.debug=${env.debug};`,
    'const m=window.site.manifest={css:{},js:{}};',
  ];
  for (const [name, info] of Object.entries(current.js)) {
    if (!/chunk\.[A-Z0-9]{8}/.test(name)) clientJs.push(`m.js['${name}']='${info.hash}';`);
  }
  for (const [name, info] of Object.entries(current.css)) {
    clientJs.push(`m.css['${name}']='${info.hash}';`);
  }

  const clientManifest = clientJs.join('\n');
  const hash = crypto.createHash('sha256').update(clientManifest).digest('hex').slice(0, 8);
  const serverManifest = { js: { ...current.js, manifest: hash }, css: { ...current.css } };

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
  const srcMapMove = env.prod
    ? Promise.resolve()
    : fs.promises.rename(`${src}.map`, path.join(env.cssDir, `${basename}.map`));
  await fs.promises.rename(src, path.join(env.cssDir, `${basename}.${hash}.css`));
  await srcMapMove;
  return [path.basename(src, '.css'), hash];
}

function parsePath(path: string) {
  const matchers = {
    css: /\.\.\/\.\.\/public\/css\/(.*)\.([A-Z0-9]+)\.css$/,
    js: /\.\.\/\.\.\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/,
  };
  for (const [type, re] of Object.entries(matchers)) {
    const match = path.match(re);
    if (match) return { type, name: match[1], hash: match[2] };
  }
  return undefined;
}

function equivalent(a: any, b: any) {
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) return a.length === b.length && a.every(x => b.includes(x)); // don't care about order
  if (typeof a !== 'object') return a === b;
  for (const key in a) {
    if (!(key in b) || !equivalent(a[key], b[key])) return false;
  }
  return true;
}
