import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body></body></html>', {
  url: 'https://lichess.org',
  pretendToBeVisual: true,
});
const { window } = dom;

const define = (k: any, v: any) => {
  try {
    Object.defineProperty(globalThis, k, { value: v, configurable: true, writable: true });
  } catch {
    delete globalThis[k];
    Object.defineProperty(globalThis, k, { value: v, configurable: true, writable: true });
  }
};

// minimal stubs
define('$html', (s: TemplateStringsArray, ..._: any[]) => s[0]);
define('$trim', (s: TemplateStringsArray, ..._: any[]) => s[0]);
define('window', window);
define('document', window.document);
define('navigator', window.navigator);
define('HTMLElement', window.HTMLElement);
define('Node', window.Node);
define('localStorage', window.localStorage);
define('sessionStorage', window.sessionStorage);

if (!('requestAnimationFrame' in globalThis))
  define('requestAnimationFrame', (cb: any) => setTimeout(() => cb(Date.now()), 0));
if (!('cancelAnimationFrame' in globalThis)) define('cancelAnimationFrame', id => clearTimeout(id));
if (!('matchMedia' in globalThis))
  define('matchMedia', (q: any) => ({
    matches: false,
    media: q,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {},
    onchange: null,
    dispatchEvent() {
      return false;
    },
  }));
