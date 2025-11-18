import { JSDOM } from 'jsdom';

const { window } = new JSDOM('<!doctype html><html><body></body></html>', {
  url: 'https://lichess.org',
  pretendToBeVisual: true,
});

define('$html', (s: TemplateStringsArray, ..._: any[]) => s[0]);
define('$trim', (s: TemplateStringsArray, ..._: any[]) => s[0]);
define('window', window);
define('document', window.document);
define('navigator', window.navigator);
define('HTMLElement', window.HTMLElement);
define('Node', window.Node);
define('localStorage', window.localStorage);
define('sessionStorage', window.sessionStorage);
define('requestAnimationFrame', (cb: any) => setTimeout(() => cb(Date.now()), 0));
define('cancelAnimationFrame', (id: any) => clearTimeout(id));
define('matchMedia', (q: any) => ({
  matches: false,
  media: q,
  addEventListener() {},
  removeEventListener() {},
  addListener() {},
  removeListener() {},
  onchange: null,
  dispatchEvent: () => false,
}));
define(
  'i18n',
  new Proxy(Object.create(null), {
    get(_root, ns: string) {
      return new Proxy(Object.create(null), {
        get(_nsObj, key: string) {
          const path = `${ns}.${key}`;
          const fn = ((...args: unknown[]) =>
            `${path}(${args.map(a => JSON.stringify(a)).join(', ')})`) as I18nFormat;
          fn.asArray = (...args: any[]) => [path, ...args.map(arg => JSON.stringify(arg))];
          (fn as any).toString = () => path;
          return fn as string & I18nFormat;
        },
      });
    },
  }),
);
define('site', {
  sri: '_test_sri_',
});

function define(k: any, v: any) {
  delete (globalThis as any)[k];
  Object.defineProperty(globalThis, k, { value: v, configurable: true, writable: true });
}
