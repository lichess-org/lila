import * as xhr from 'common/xhr';
import { supportsSystemTheme } from 'common/theme';

export const assetUrl = (path: string, opts: AssetUrlOpts = {}) => {
  opts = opts || {};
  const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
    version = opts.version || document.body.getAttribute('data-asset-version');
  return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
};

const loadedCss = new Map<string, Promise<void>>();
export const loadCss = (url: string, media?: 'dark' | 'light'): Promise<void> => {
  if (!loadedCss.has(url)) {
    const el = document.createElement('link');
    el.rel = 'stylesheet';
    el.href = assetUrl(lichess.debug ? `${url}?_=${Date.now()}` : url);
    if (media) el.media = `(prefers-color-scheme: ${media})`;
    loadedCss.set(
      url,
      new Promise<void>(resolve => {
        el.onload = () => resolve();
      }),
    );
    document.head.append(el);
  }
  return loadedCss.get(url)!;
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

export const loadIife = (url: string, opts: AssetUrlOpts = {}): Promise<void> => {
  if (!scriptCache.has(url)) scriptCache.set(url, xhr.script(assetUrl(url, opts)));
  return scriptCache.get(url)!;
};

export async function loadEsm<T, ModuleOpts = any>(
  name: string,
  opts?: { init?: ModuleOpts; url?: AssetUrlOpts },
): Promise<T> {
  const module = await import(assetUrl(jsModule(name), opts?.url));
  return module.initModule ? module.initModule(opts?.init) : module.default(opts?.init);
}

export const userComplete = async (opts: UserCompleteOpts): Promise<UserComplete> => {
  loadCssPath('complete');
  return loadEsm('userComplete', { init: opts });
};

export const hopscotch = () => {
  loadCss('npm/hopscotch/dist/css/hopscotch.min.css');
  return loadIife('npm/hopscotch/dist/js/hopscotch.min.js', {
    noVersion: true,
  });
};

export const embedChessground = () => import(assetUrl('npm/chessground.min.js'));
