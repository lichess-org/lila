import { script as xhrScript } from 'lib/xhr';
import { memoize } from 'lib';

export const baseUrl = memoize(() => document.body.getAttribute('data-asset-url') || '');

const assetVersion = memoize(() => document.body.getAttribute('data-asset-version'));

export const url = (path: string, opts: AssetUrlOpts = {}) => {
  const base = opts.documentOrigin ? window.location.origin : opts.pathOnly ? '' : baseUrl();
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
  return `hashed/${extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`}`;
}

// bump flairs version if a flair is changed only (not added or removed)
export const flairSrc = (flair: Flair) => url(`flair/img/${flair}.webp`, { pathVersion: '_____4' });

// bump fide fed version if a fide fed is changed only (not added or removed)
export const fideFedSrc = (fideFed: FideFed) =>
  url(`fide/fed-webp/${fideFed}.webp`, { pathVersion: '_____2' });

export const loadCss = (href: string, key?: string): Promise<void> => {
  return new Promise(resolve => {
    href = href.startsWith('https:') ? href : url(href);
    if (document.head.querySelector(`link[href="${href}"]`)) return resolve();

    const el = document.createElement('link');
    if (key) el.setAttribute('data-css-key', key);
    el.rel = 'stylesheet';
    el.href = href;
    el.onload = () => resolve();
    document.head.append(el);
  });
};

export const loadCssPath = async (key: string): Promise<void> => {
  const hash = site.manifest.css[key];
  await loadCss(`css/${key}${hash ? `.${hash}` : ''}.css`, key);
};

export const removeCss = (href: string) => $(`head > link[href="${href}"]`).remove();

export const removeCssPath = (key: string) => $(`head > link[data-css-key="${key}"]`).remove();

export const jsModule = (name: string, prefix: string = 'compiled/') => {
  if (name.endsWith('.js')) name = name.slice(0, -3);
  const hash = site.manifest.js[name];
  return `${prefix}${name}${hash ? `.${hash}` : ''}.js`;
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
  const s = window.site;
  const path = `compiled/i18n/${catalog}.${s.displayLocale}.${s.manifest.i18n![catalog]}.js`;
  await import(url(path));
};

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}

export const loadPieces = new Promise<void>((resolve, reject) => {
  if (document.getElementById('main-wrap')?.classList.contains('is3d')) return resolve();
  const style = window.getComputedStyle(document.body);
  const urls = ['white', 'black']
    .flatMap(c => ['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'].map(r => `---${c}-${r}`))
    .map(
      u =>
        style
          .getPropertyValue(u)
          .slice(4, -1) // strip 'url(' + ... + ')'
          .replace(/\\([:/.])/g, '$1'), // webkit escapes
    )
    .filter(x => x);
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
