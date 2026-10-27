import { COLORS } from 'chessops';

import { memoize } from 'lib';
import { script as xhrScript } from 'lib/xhr';

export const baseUrl = memoize(() => document.body.getAttribute('data-asset-url') || '');

const assetVersion = memoize(() => document.body.getAttribute('data-asset-version'));

export const url = (path: string, opts: AssetUrlOpts = {}) => {
  const base = opts.pathOnly ? '' : baseUrl();
  const pathVersion = !opts.pathVersion
    ? ''
    : opts.pathVersion === true
      ? `_${assetVersion()}/`
      : `_${opts.pathVersion}/`;
  const hash = !pathVersion && site.manifest.hashed[path];
  return `${base}/assets/${hash ? asHashed(path, hash) : `${pathVersion}${path}`}`;
};

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.lastIndexOf('.');
  return `hashed/${extPos === -1 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`}`;
}

// bump flairs version if a flair is changed only (not added or removed)
export const flairSrc = (flair: Flair) => url(`flair/img/${flair}.webp`, { pathVersion: '_____5' });

// bump fide fed version if a fide fed is changed only (not added or removed)
export const fideFedSrc = (fideFed: FideFed) =>
  url(`fide/fed-webp/${fideFed}.webp`, { pathVersion: '_____3' });

const resolveCss = (loc: CssLocation) =>
  'href' in loc
    ? loc.href.startsWith('https:')
      ? loc.href
      : url(loc.href)
    : url([`css/${loc.key}`, site.manifest.css[loc.key], 'css'].filter(Boolean).join('.'));

const stylesheets = new Map<string, { el: HTMLLinkElement; ready: Promise<void>; refs: number }>();

export const loadCss = (loc: CssLocation): Promise<void> => {
  const href = resolveCss(loc);
  const existing = stylesheets.get(href);
  if (existing) {
    existing.refs++;
    return existing.ready;
  }
  if (document.head.querySelector(`link[href="${href}"]`)) return Promise.resolve();

  const el = document.createElement('link');
  el.rel = 'stylesheet';
  el.href = href;
  const ready = new Promise<void>((resolve, reject) => {
    el.onload = () => resolve();
    el.onerror = () => {
      if (stylesheets.get(href)?.el === el) stylesheets.delete(href);
      el.remove();
      reject(new Error(`Failed to load CSS: ${href}`));
    };
  });
  stylesheets.set(href, { el, ready, refs: 1 });
  document.head.append(el);
  return ready;
};

export const releaseCss = (loc: CssLocation): void => {
  const href = resolveCss(loc);
  const existing = stylesheets.get(href);
  if (!existing || --existing.refs > 0) return;

  existing.el.remove();
  stylesheets.delete(href);
};

export const removeCss = (loc: CssLocation): void => {
  const href = 'href' in loc ? url(loc.href) : resolveCss(loc);
  const el = document.querySelector(`head > link[href="${href}"]`);
  if (el) el.remove();
  else document.querySelector(`head > link[data-css-key="${loc.key}"]`)?.remove();
  stylesheets.delete(href);
};

export const jsModule = (name: string, prefix = 'compiled/') => {
  if (name.endsWith('.js')) name = name.slice(0, -3);
  return [`${prefix}${name}`, site.manifest.js[name], 'js'].filter(Boolean).join('.');
};

export const loadIife = (u: string, opts: AssetUrlOpts = {}): Promise<void> => {
  return xhrScript(url(u, opts));
};

export async function loadEsm<T>(name: string, opts: EsmModuleOpts = {}): Promise<T> {
  const module = await import(url(opts.npm ? jsModule(name, 'npm/') : jsModule(name), opts));
  const initializer = module.initModule ?? module.default;
  return opts.npm && !opts.init ? initializer : initializer(opts.init);
}

export const loadEsmPage = async (name: string) => {
  const modulePromise = import(url(jsModule(name)));
  const dataScript = document.getElementById('page-init-data');
  const opts = dataScript && JSON.parse(dataScript.innerHTML);
  dataScript?.remove();
  const module = await modulePromise;
  module.initModule ? module.initModule(opts) : module.default(opts);
};

export const loadI18n = async (catalog: string) => {
  await import(document.body.dataset.i18nCatalog!);
  await import(url(`compiled/i18n/${catalog}.${window.site.manifest.i18n![catalog]}.js`));
};

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}

export const loadPieces = new Promise<void>((resolve, reject) => {
  if (document.getElementById('main-wrap')?.classList.contains('is3d')) return resolve();
  const style = window.getComputedStyle(document.body);
  const urls = COLORS.flatMap(c =>
    ['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'].map(r => `---${c}-${r}`),
  )
    .map(
      u =>
        style
          .getPropertyValue(u)
          .slice(4, -1) // strip 'url(' + ... + ')'
          .replace(/\\([:/.])/g, '$1'), // webkit escapes
    )
    .filter(Boolean);
  let assetsToDecode = urls.length;
  if (assetsToDecode === 0) return resolve();
  urls.forEach(url => {
    const img = new Image();
    img.src = url;
    img
      .decode()
      .then(() => {
        if (--assetsToDecode === 0) resolve();
      })
      .catch(reject);
  });
});
