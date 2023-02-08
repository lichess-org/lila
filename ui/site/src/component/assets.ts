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
    el.href = assetUrl(url);
    if (media) el.media = `(prefers-color-scheme: ${media})`;
    loadedCss.set(
      url,
      new Promise<void>(resolve => {
        el.onload = () => resolve();
      })
    );
    document.head.append(el);
  }
  return loadedCss.get(url)!;
};

export const loadCssPath = async (key: string): Promise<void> => {
  const theme = $('body').data('theme');
  const load = (theme: string, media?: 'dark' | 'light') =>
    loadCss(`css/${key}.${document.dir || 'ltr'}.${theme}.${$('body').data('dev') ? 'dev' : 'min'}.css`, media);
  if (theme === 'system') {
    if (supportsSystemTheme()) {
      await load('dark', 'dark');
      await load('light', 'light');
    } else {
      await load('dark');
    }
  } else await load(theme);
};

export const jsModule = (name: string) => `compiled/${name}${$('body').data('dev') ? '' : '.min'}.js`;

const loadedScript = new Map<string, Promise<void>>();
export const loadScript = (url: string, opts: AssetUrlOpts = {}): Promise<void> => {
  if (!loadedScript.has(url)) loadedScript.set(url, xhr.script(assetUrl(url, opts)));
  return loadedScript.get(url)!;
};

export const loadModule = (name: string): Promise<void> => loadScript(jsModule(name));
export const loadIife = async (name: string, iife: keyof Window) => {
  await loadModule(name);
  return window[iife];
};

export const userComplete = async (): Promise<UserComplete> => {
  loadCssPath('complete');
  await loadModule('userComplete');
  return window.UserComplete;
};

export const hopscotch = () => {
  loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
  return loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {
    noVersion: true,
  });
};
