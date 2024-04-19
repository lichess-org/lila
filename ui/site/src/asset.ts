import * as xhr from 'common/xhr';
import { memoize } from 'common';

export const baseUrl = memoize(() => document.body.getAttribute('data-asset-url') || '');

const version = memoize(() => document.body.getAttribute('data-asset-version'));

export const url = (path: string, opts: AssetUrlOpts = {}) => {
  opts = opts || {};
  const base = opts.sameDomain ? '' : baseUrl(),
    v = opts.version || version();
  return `${base}/assets${opts.noVersion ? '' : '/_' + v}/${path}`;
};

// bump flairs version if a flair is changed only (not added or removed)
export const flairSrc = (flair: Flair) => url(`flair/img/${flair}.webp`, { version: '_____2' });

export const loadCss = (href: string, key?: string): Promise<void> =>
  new Promise(resolve => {
    href = url(href, { noVersion: true });
    if (key) removeCssPath(key);
    else if (document.querySelector(`head > link[href="${href}"]`)) return resolve();

    const el = document.createElement('link');
    if (key) el.className = `css-${key}`;
    el.rel = 'stylesheet';
    el.href = href;
    el.onload = () => resolve();
    document.head.append(el);
  });

export const loadCssPath = async (key: string): Promise<void> => {
  const hash = site.manifest.css[key];
  await loadCss(`css/${key}${hash ? `.${hash}` : ''}.css`, key);
};

export const removeCssPath = (key: string) => {
  $(`head > link.css-${key}`).remove();
};

export const jsModule = (name: string) => {
  if (name.endsWith('.js')) name = name.slice(0, -3);
  const hash = site.manifest.js[name];
  return `compiled/${name}${hash ? `.${hash}` : ''}.js`;
};

const scriptCache = new Map<string, Promise<void>>();

export const loadIife = (u: string, opts: AssetUrlOpts = {}): Promise<void> => {
  if (!scriptCache.has(u)) scriptCache.set(u, xhr.script(url(u, opts)));
  return scriptCache.get(u)!;
};

export async function loadEsm<T, ModuleOpts = any>(
  name: string,
  opts?: { init?: ModuleOpts; url?: AssetUrlOpts },
): Promise<T> {
  const urlOpts = opts?.url?.version ? opts?.url : { ...opts?.url, noVersion: true };
  const module = await import(url(jsModule(name), urlOpts));
  return module.initModule ? module.initModule(opts?.init) : module.default(opts?.init);
}

export const loadPageEsm = async (name: string) => {
  const modulePromise = import(url(jsModule(name), { noVersion: true }));
  const dataScript = document.getElementById('page-init-data');
  const opts = dataScript && JSON.parse(dataScript.innerHTML);
  dataScript?.remove();
  const module = await modulePromise;
  module.initModule ? module.initModule(opts) : module.default(opts);
};

export const userComplete = async (opts: UserCompleteOpts): Promise<UserComplete> => {
  const [userComplete] = await Promise.all([
    loadEsm('bits.userComplete', { init: opts }),
    loadCssPath('complete'),
  ]);
  return userComplete as UserComplete;
};

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}
