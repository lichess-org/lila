import cps from 'node:child_process';
import path from 'node:path';
import fs from 'node:fs';
import crypto from 'node:crypto';
import es from 'esbuild';
import { env, colors as c, warnMark } from './main.ts';
import { globArray, globArrays } from './parse.ts';
import { isUnmanagedAsset } from './copies.ts';
import { allSources } from './sass.ts';
import { jsLogger } from './console.ts';

type Manifest = { [key: string]: { hash?: string; imports?: string[]; mtime?: number } };

const current: { js: Manifest; css: Manifest; hashed: Manifest; dirty: boolean } = {
  js: {},
  css: {},
  hashed: {},
  dirty: false,
};

let writeTimer: NodeJS.Timeout;

export function writeManifest(): void {
  if (!current.dirty) return;
  clearTimeout(writeTimer);
  writeTimer = setTimeout(write, 500);
}

export function jsManifest(meta: es.Metafile): void {
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
  if (isEquivalent(newJsManifest, current.js) && fs.existsSync(env.manifestFile)) return;
  current.js = shallowSort({ ...current.js, ...newJsManifest });
  current.dirty = true;
}

export async function cssManifest(): Promise<void> {
  const files = await globArray(path.join(env.cssTempDir, '*.css'));
  const css: { name: string; hash: string }[] = await Promise.all(files.map(hashMoveCss));
  const newCssManifest: Manifest = {};
  for (const { name, hash } of css) newCssManifest[name] = { hash };
  if (isEquivalent(newCssManifest, current.css)) return;
  current.css = shallowSort({ ...current.css, ...newCssManifest });
  current.dirty = true;
  writeManifest();
}

export async function hashedManifest(): Promise<void> {
  const newHashLinks = new Map<string, number>();
  const alreadyHashed = new Map<string, string>();
  const sources: string[] = (
    await globArrays(
      env.building.flatMap(x => x.hashGlobs ?? []),
      { cwd: env.outDir },
    )
  ).filter(isUnmanagedAsset);
  const sourceStats = await Promise.all(sources.map(file => fs.promises.stat(file)));

  for (const [i, stat] of sourceStats.entries()) {
    const name = sources[i].slice(env.outDir.length + 1);

    if (stat.mtimeMs === current.hashed[name]?.mtime) alreadyHashed.set(name, current.hashed[name].hash!);
    else newHashLinks.set(name, stat.mtimeMs);
  }
  await Promise.allSettled([...alreadyHashed].map(([name, hash]) => link(name, hash)));

  for (const { name, hash } of await Promise.all([...newHashLinks.keys()].map(hashLink))) {
    current.hashed[name] = Object.defineProperty({ hash }, 'mtime', { value: newHashLinks.get(name) });
  }

  if (newHashLinks.size === 0 && alreadyHashed.size === Object.keys(current.hashed).length) return;

  for (const name of Object.keys(current.hashed)) {
    if (!sources.some(x => x.endsWith(name))) delete current.hashed[name];
  }
  current.dirty = true;
  writeManifest();
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
    'const m=window.site.manifest={css:{},js:{},hashed:{}};',
  ];
  if (env.remoteLog) clientJs.push(jsLogger());
  for (const [name, info] of Object.entries(current.js)) {
    if (!/common\.[A-Z0-9]{8}/.test(name)) clientJs.push(`m.js['${name}']='${info.hash}';`);
  }
  for (const [name, info] of Object.entries(current.css)) {
    clientJs.push(`m.css['${name}']='${info.hash}';`);
  }
  for (const [path, info] of Object.entries(current.hashed)) {
    clientJs.push(`m.hashed[${JSON.stringify(path)}]='${info.hash}';`);
  }
  const hashable = clientJs.join('\n');
  const hash = crypto.createHash('sha256').update(hashable).digest('hex').slice(0, 8);
  // add the date after hashing
  const clientManifest =
    hashable +
    `\nwindow.site.info.date='${
      new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00'
    }';\n`;
  const serverManifest = {
    js: { manifest: { hash }, ...current.js },
    css: { ...current.css },
    hashed: { ...current.hashed },
  };

  await Promise.all([
    fs.promises.writeFile(path.join(env.jsOutDir, `manifest.${hash}.js`), clientManifest),
    fs.promises.writeFile(
      path.join(env.jsOutDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`),
      JSON.stringify(serverManifest, null, env.prod ? undefined : 2),
    ),
  ]);
  current.dirty = false;
  env.log(`Manifest hash ${c.green(hash)}`);
}

async function hashMoveCss(src: string) {
  const content = await fs.promises.readFile(src, 'utf-8');
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 8);
  const basename = path.basename(src, '.css');
  await Promise.allSettled([
    env.prod ? undefined : fs.promises.rename(`${src}.map`, path.join(env.cssOutDir, `${basename}.css.map`)),
    fs.promises.rename(src, path.join(env.cssOutDir, `${basename}.${hash}.css`)),
  ]);
  return { name: path.basename(src, '.css'), hash };
}

async function hashLink(name: string) {
  const src = path.join(env.outDir, name);
  const hash = crypto
    .createHash('sha256')
    .update(await fs.promises.readFile(src))
    .digest('hex')
    .slice(0, 8);
  link(name, hash);
  return { name, hash };
}

async function isComplete() {
  for (const bundle of [...env.packages.values()].map(x => x.bundles ?? []).flat()) {
    const name = path.basename(bundle, '.ts');
    if (!current.js[name]) {
      env.log(`${warnMark} - No manifest without building '${c.cyan(name + '.ts')}'`);
      return false;
    }
  }
  for (const css of await allSources()) {
    const name = path.basename(css, '.scss');
    if (!current.css[name]) {
      env.log(`${warnMark} - No manifest without building '${c.cyan(name + '.scss')}'`);
      return false;
    }
  }
  return true;
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

function isEquivalent(a: any, b: any): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a))
    return Array.isArray(b) && a.length === b.length && a.every(x => b.find((y: any) => isEquivalent(x, y)));
  if (typeof a !== 'object') return false;
  const [aKeys, bKeys] = [Object.keys(a), Object.keys(b)];
  if (aKeys.length !== bKeys.length) return false;
  for (const key of aKeys) {
    if (!bKeys.includes(key) || !isEquivalent(a[key], b[key])) return false;
  }
  return true;
}

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.indexOf('.');
  return extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`;
}

function link(name: string, hash: string) {
  const link = path.join(env.hashOutDir, asHashed(name, hash));
  fs.promises.symlink(path.join('..', name), link).catch(() => {});
}
