import cps from 'node:child_process';
import fs from 'node:fs';
import crypto from 'node:crypto';
import { join } from 'node:path';
import { env, c } from './env.ts';
import { jsLogger } from './console.ts';
import { taskOk } from './task.ts';
import { shallowSort, isContained } from './algo.ts';

const manifest = {
  i18n: {} as Manifest,
  js: {} as Manifest,
  css: {} as Manifest,
  hashed: {} as Manifest,
  dirty: false,
};
let writeTimer: NodeJS.Timeout;

type SplitAsset = { hash?: string; path?: string; imports?: string[]; inline?: string };

export type Manifest = { [key: string]: SplitAsset };
export type ManifestUpdate = Partial<Omit<typeof manifest, 'dirty'>>;

export function stopManifest(clear = false): void {
  clearTimeout(writeTimer);
  if (clear) {
    manifest.i18n = {};
    manifest.js = {};
    manifest.css = {};
    manifest.hashed = {};
    manifest.dirty = false;
  }
}

export function updateManifest(update: ManifestUpdate = {}): void {
  for (const [key, partial] of Object.entries(update) as [keyof ManifestUpdate, Manifest][]) {
    const full = manifest[key];
    if (isContained(full, partial)) continue;
    for (const [name, entry] of Object.entries(partial)) {
      full[name] = shallowSort({ ...full[name], ...entry });
    }
    manifest[key] = shallowSort(full);
    manifest.dirty = true;
  }
  if (manifest.dirty) {
    clearTimeout(writeTimer);
    writeTimer = setTimeout(writeManifest, env.watch && !env.startTime ? 750 : 0);
  }
}

async function writeManifest() {
  if (!(env.manifest && taskOk())) return;
  const commitMessage = cps
    .execSync('git log -1 --pretty=%s', { encoding: 'utf-8' })
    .trim()
    .replaceAll("'", '&#39;')
    .replaceAll('"', '&quot;');

  const clientJs: string[] = [
    'if (!window.site) window.site={};',
    'if (!window.site.info) window.site.info={};',
    `window.site.info.commit='${cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim()}';`,
    `window.site.info.message='${commitMessage}';`,
    `window.site.debug=${env.debug};`,
  ];
  if (env.remoteLog) clientJs.push(jsLogger());

  const pairLine = ([name, info]: [string, SplitAsset]) => `'${name.replaceAll("'", "\\'")}':'${info.hash}'`;
  const jsLines = Object.entries(manifest.js)
    .filter(([name, _]) => !/common\.[A-Z0-9]{8}/.test(name))
    .map(pairLine)
    .join(',');
  const cssLines = Object.entries(manifest.css).map(pairLine).join(',');
  const hashedLines = Object.entries(manifest.hashed).map(pairLine).join(',');

  clientJs.push(`window.site.manifest={\ncss:{${cssLines}},\njs:{${jsLines}},\nhashed:{${hashedLines}}\n};`);

  const hashable = clientJs.join('\n');
  const hash = crypto.createHash('sha256').update(hashable).digest('hex').slice(0, 8);

  const clientManifest =
    hashable +
    `\nwindow.site.info.date='${
      new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00'
    }';\n`;
  const serverManifest = JSON.stringify(
    {
      js: { manifest: { hash }, ...manifest.js, ...manifest.i18n },
      css: { ...manifest.css },
      hashed: { ...manifest.hashed },
    },
    null,
    env.prod ? undefined : 2,
  );
  await Promise.all([
    fs.promises.writeFile(join(env.jsOutDir, `manifest.${hash}.js`), clientManifest),
    fs.promises.writeFile(join(env.jsOutDir, `manifest.${env.prod ? 'prod' : 'dev'}.json`), serverManifest),
  ]);
  manifest.dirty = false;
  const serverHash = crypto.createHash('sha256').update(serverManifest).digest('hex').slice(0, 8);
  env.log(
    `'${c.cyan(`public/compiled/manifest.${hash}.js`)}', '${c.cyan(`public/compiled/manifest.${env.prod ? 'prod' : 'dev'}.json`)}' ${c.grey(serverHash)}`,
    'manifest',
  );
}
