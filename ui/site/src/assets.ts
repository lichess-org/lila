import * as xhr from 'common/xhr';
import { supportsSystemTheme } from 'common/theme';
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

const loadedCss = new Map<string, Promise<void>>();
export const loadCss = (href: string, media?: 'dark' | 'light'): Promise<void> => {
  if (!loadedCss.has(href)) {
    const el = document.createElement('link');
    el.rel = 'stylesheet';
    el.href = url(site.debug ? `${href}?_=${Date.now()}` : href);
    if (media) el.media = `(prefers-color-scheme: ${media})`;
    loadedCss.set(
      href,
      new Promise<void>(resolve => {
        el.onload = () => resolve();
      }),
    );
    document.head.append(el);
  }
  return loadedCss.get(href)!;
};

export const loadCssPath = async (key: string): Promise<void> => {
  const theme = document.body.dataset.theme!;
  const load = (theme: string, media?: 'dark' | 'light') =>
    loadCss(
      `css/${key}.${document.dir || 'ltr'}.${theme}.${document.body.dataset.dev ? 'dev' : 'min'}.css`,
      media,
    );
  if (theme === 'system') {
    if (supportsSystemTheme()) {
      await Promise.all([load('dark', 'dark'), load('light', 'light')]);
    } else {
      await load('dark');
    }
  } else await load(theme);
};

export const jsModule = (name: string) => `compiled/${name}${document.body.dataset.dev ? '' : '.min'}.js`;

const scriptCache = new Map<string, Promise<void>>();

export const loadIife = (u: string, opts: AssetUrlOpts = {}): Promise<void> => {
  if (!scriptCache.has(u)) scriptCache.set(u, xhr.script(url(u, opts)));
  return scriptCache.get(u)!;
};

export async function loadEsm<T, ModuleOpts = any>(
  name: string,
  opts?: { init?: ModuleOpts; url?: AssetUrlOpts },
): Promise<T> {
  const module = await import(url(jsModule(name), opts?.url));
  return module.initModule ? module.initModule(opts?.init) : module.default(opts?.init);
}

export const userComplete = async (opts: UserCompleteOpts): Promise<UserComplete> => {
  const [userComplete] = await Promise.all([
    loadEsm('userComplete', { init: opts }),
    loadCssPath('complete'),
  ]);
  return userComplete as UserComplete;
};

export const hopscotch = () => {
  return Promise.all([
    loadCss('npm/hopscotch/dist/css/hopscotch.min.css'),
    loadIife('npm/hopscotch/dist/js/hopscotch.min.js', {
      noVersion: true,
    }),
  ]);
};

export function embedChessground() {
  return import(url('npm/chessground.min.js'));
}
