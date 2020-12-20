import * as xhr from 'common/xhr';

export const assetUrl = (path: string, opts: AssetUrlOpts = {}) => {
  opts = opts || {};
  const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
    version = opts.version || document.body.getAttribute('data-asset-version');
  return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
};

export const soundUrl = assetUrl('sound', { version: '000003' });

const loadedCss = new Map();

export const loadCss = (url: string) => {
  if (!loadedCss.has(url)) {
    loadedCss.set(url, true);
    const el = document.createElement('link');
    el.rel = 'stylesheet';
    el.href = assetUrl(url);
    document.head.append(el);
  }
};

export const loadCssPath = (key: string) =>
  loadCss(`css/${key}.${$('body').data('theme')}.${$('body').data('dev') ? 'dev' : 'min'}.css`);

export const jsModule = (name: string) =>
  `compiled/${name}${$('body').data('dev') ? '' : '.min'}.js`;

export const loadScript = (url: string, opts: AssetUrlOpts = {}): Promise<void> =>
  xhr.script(assetUrl(url, opts));

export const hopscotch = () => {
  loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
  return loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {
    noVersion: true
  })
};

export const slider = () =>
  loadScript(
    'javascripts/vendor/jquery-ui.slider' + ('ontouchstart' in window ? '.touch' : '') + '.min.js'
  );
