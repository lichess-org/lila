import * as xhr from 'common/xhr';
import { memoize } from 'common';

export const baseUrl = memoize(() => document.body.getAttribute('data-asset-url') || '');

const assetVersion = memoize(() => document.body.getAttribute('data-asset-version'));

export const url = (path: string, opts: AssetUrlOpts = {}) => {
  const base = opts.documentOrigin ? window.location.origin : opts.pathOnly ? '' : baseUrl();
  const version = opts.version === false ? '' : `_${opts.version ?? assetVersion()}/`;
  const hash = opts.version !== false && site.manifest.hashed[path];
  return `${base}/assets/${hash ? asHashed(path, hash) : `${version}${path}`}`;
};

function asHashed(path: string, hash: string) {
  const name = path.slice(path.lastIndexOf('/') + 1);
  const extPos = name.indexOf('.');
  return `hashed/${extPos < 0 ? `${name}.${hash}` : `${name.slice(0, extPos)}.${hash}${name.slice(extPos)}`}`;
}

// bump flairs version if a flair is changed only (not added or removed)
export const flairSrc = (flair: Flair) => url(`flair/img/${flair}.webp`, { version: '_____2' });

export const loadCss = (href: string, key?: string): Promise<void> => {
  return new Promise(resolve => {
    href = url(href, { version: false });
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
  if (!scriptCache.has(u)) scriptCache.set(u, xhr.script(url(u, opts)));
  return scriptCache.get(u)!;
};

export async function loadEsm<T>(name: string, opts: EsmModuleOpts = {}): Promise<T> {
  opts = { ...opts, version: opts.version ?? false };

  const module = await import(url(opts.npm ? jsModule(name, 'npm/') : jsModule(name), opts));
  const initializer = module.initModule ?? module.default;
  return opts.npm && !opts.init ? initializer : initializer(opts.init);
}

export const loadPageEsm = async (name: string) => {
  const modulePromise = import(url(jsModule(name), { version: false }));
  const dataScript = document.getElementById('page-init-data');
  const opts = dataScript && JSON.parse(dataScript.innerHTML);
  dataScript?.remove();
  const module = await modulePromise;
  module.initModule ? module.initModule(opts) : module.default(opts);
};

export const userComplete = async (opts: UserCompleteOpts): Promise<UserComplete> => {
  const [userComplete] = await Promise.all([
    loadEsm('bits.userComplete', { init: opts }),
    loadCssPath('bits.complete'),
  ]);
  return userComplete as UserComplete;
};

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}
