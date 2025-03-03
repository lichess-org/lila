import { script as xhrScript } from 'common/xhr';
import { memoize } from 'common';

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
export const flairSrc = (flair: Flair) => url(`flair/img/${flair}.webp`, { pathVersion: '_____2' });

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

const scriptCache = new Map<string, Promise<void>>();

export const loadIife = (u: string, opts: AssetUrlOpts = {}): Promise<void> => {
  if (!scriptCache.has(u)) scriptCache.set(u, xhrScript(url(u, opts)));
  return scriptCache.get(u)!;
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

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}

let isWorkerPatched = false;

export function patchWorkerConstructor() {
  // this might be the cleanest way to bootstrap emscripted wasm as es6 with a different link.
  // libpthread.js:allocateUnusedWorker is hardwired to import the bare module script
  // filename relative to import.meta.url, but we need it hashed for auto-versioning
  if (isWorkerPatched) return;
  isWorkerPatched = true;

  window.Worker = class extends window.Worker {
    constructor(urlOrStr: string | URL, opts?: WorkerOptions) {
      const url = new URL(urlOrStr.toString());
      const file = url.pathname.split('/').pop();
      if (file?.endsWith('.js') && (url.host === location.host || url.origin === baseUrl())) {
        const key = Object.keys(site.manifest.hashed).find(k => k.endsWith(file));
        if (key) {
          super(new URL(`assets/${asHashed(key, site.manifest.hashed[key])}`, url.origin), opts);
          return;
        }
      }
      super(urlOrStr, opts);
    }
  };
}
