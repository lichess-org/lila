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

function define(k: any, v: any) {
  delete globalThis[k];
  Object.defineProperty(globalThis, k, { value: v, configurable: true, writable: true });
}
