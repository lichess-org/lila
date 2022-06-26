import * as xhr from 'common/xhr';

export const assetUrl = (path: string, opts: AssetUrlOpts = {}) => {
  opts = opts || {};
  const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
    version = opts.version || document.body.getAttribute('data-asset-version');
  return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
};

const loadedCss = new Map<string, Promise<void>>();
export const loadCss = (url: string): Promise<void> => {
  if (!loadedCss.has(url)) {
    const el = document.createElement('link');
    el.rel = 'stylesheet';
    el.href = assetUrl(url);
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

export const loadCssPath = (key: string): Promise<void> =>
  loadCss(
    `css/${key}.${document.dir || 'ltr'}.${$('body').data('theme')}.${$('body').data('dev') ? 'dev' : 'min'}.css`
  );

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
