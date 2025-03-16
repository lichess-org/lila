import { type Hooks } from 'snabbdom';
import { memoize } from './common';
import { bind } from './snabbdom';
import * as licon from './licon';

const longPressDuration = 610;

export function bindMobileTapHold(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void): void {
  let longPressCountdown: Timeout;

  el.addEventListener('touchstart', e => {
    longPressCountdown = setTimeout(() => {
      f(e);
      if (redraw) redraw();
    }, longPressDuration);
  });

  el.addEventListener('touchmove', () => clearTimeout(longPressCountdown));

  el.addEventListener('touchcancel', () => clearTimeout(longPressCountdown));

  el.addEventListener('touchend', () => clearTimeout(longPressCountdown));
}

export function isBrowserSupported(): boolean {
  // when feature detection is not enough
  if (isSafari({ below: '15.4' })) return false;
  return true; // TODO add unsupported browsers
}

export const bindMobileMousedown =
  (f: (e: Event) => unknown, redraw?: () => void) =>
  (el: HTMLElement): void => {
    for (const mousedownEvent of ['touchstart', 'mousedown']) {
      el.addEventListener(
        mousedownEvent,
        e => {
          f(e);
          e.preventDefault();
          if (redraw) redraw();
        },
        { passive: false },
      );
    }
  };

export const hookMobileMousedown = (f: (e: Event) => any): Hooks =>
  bind('ontouchstart' in window ? 'click' : 'mousedown', f);

export const prefersLightThemeQuery = (): MediaQueryList =>
  window.matchMedia('(prefers-color-scheme: light)');

export const currentTheme = (): 'light' | 'dark' => {
  const dataTheme = document.body.dataset.theme!;
  if (dataTheme === 'system') return prefersLightThemeQuery().matches ? 'light' : 'dark';
  else if (dataTheme === 'light') return 'light';
  else return 'dark';
};

let col1cache: 'init' | 'rec' | boolean = 'init';

export function isCol1(): boolean {
  if (typeof col1cache === 'string') {
    if (col1cache === 'init') {
      // only once
      window.addEventListener('resize', () => {
        col1cache = 'rec';
      }); // recompute on resize
      if (navigator.userAgent.indexOf('Edge/') > -1)
        // edge gets false positive on page load, fix later
        requestAnimationFrame(() => {
          col1cache = 'rec';
        });
    }
    col1cache = !!window.getComputedStyle(document.body).getPropertyValue('---col1');
  }
  return col1cache;
}

const lowerAgent = navigator.userAgent.toLowerCase();

export const isTouchDevice = (): boolean => !hasMouse(); // prefer isTouchDevice() to below matches

export const isMobile = (): boolean => isAndroid() || isIos();

export const isAndroid: () => boolean = memoize(() => lowerAgent.includes('android'));

export const isIos: () => boolean = memoize(() => /iphone|ipod/.test(lowerAgent) || isIPad());

export const isIPad = (): boolean => navigator?.maxTouchPoints > 2 && /ipad|macintosh/.test(lowerAgent);

export type VersionConstraint = { atLeast?: string; below?: string }; // '11', '14.1.x', '127_2_7'

export const isChrome = (constraint?: VersionConstraint): boolean =>
  isVersionCompatible(lowerAgent.match(/chrome\/(.*)/)?.[1], constraint);

export const isFirefox = (constraint?: VersionConstraint): boolean =>
  isVersionCompatible(lowerAgent.match(/firefox\/(.*)/)?.[1], constraint);

export const isSafari = (constraint?: VersionConstraint): boolean =>
  lowerAgent.includes('version/') && isVersionCompatible(webkitVersion(), constraint);

export const isIosSafari = (constraint?: VersionConstraint): boolean => isIos() && isSafari(constraint);

export const isWebkit = (constraint?: VersionConstraint): boolean =>
  isVersionCompatible(webkitVersion(), constraint);

export const isIosChrome = (constraint?: VersionConstraint): boolean =>
  lowerAgent.includes('crios/') && isVersionCompatible(webkitVersion(), constraint);

export const isApple: () => boolean = memoize<boolean>(
  () => /macintosh|iphone|ipad|ipod/.test(lowerAgent), // macOS or iOS
);

const webkitVersion = memoize<string | false>(
  () =>
    (lowerAgent.includes('safari') &&
      !lowerAgent.includes('chrome') &&
      !lowerAgent.includes('android') &&
      (lowerAgent.match(/version\/(.*)/)?.[1] ||
        (lowerAgent.includes('crios/') && lowerAgent.match(/ os ((?:\d+[._]?){1,3})/i)?.[1]))) ||
    false,
);

export const shareIcon: () => string = () => (isApple() ? licon.ShareIos : licon.ShareAndroid);

export type Feature =
  | 'wasm'
  | 'sharedMem'
  | 'simd'
  | 'dynamicImportFromWorker'
  | 'bigint'
  | 'structuredClone';

export const hasFeature = (feat?: Feature): boolean => !feat || features().includes(feat as Feature);

export const features: () => readonly Feature[] = memoize<readonly Feature[]>(() => {
  const features: Feature[] = [];
  if (typeof BigInt === 'function') features.push('bigint');
  if (typeof structuredClone !== 'undefined') features.push('structuredClone');
  if (
    typeof WebAssembly === 'object' &&
    typeof WebAssembly.validate === 'function' &&
    WebAssembly.validate(Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]))
  ) {
    features.push('wasm');
    // i32x4.dot_i16x8_s, i32x4.trunc_sat_f64x2_u_zero
    const sourceWithSimd = Uint8Array.from([
      0, 97, 115, 109, 1, 0, 0, 0, 1, 12, 2, 96, 2, 123, 123, 1, 123, 96, 1, 123, 1, 123, 3, 3, 2, 0, 1, 7, 9,
      2, 1, 97, 0, 0, 1, 98, 0, 1, 10, 19, 2, 9, 0, 32, 0, 32, 1, 253, 186, 1, 11, 7, 0, 32, 0, 253, 253, 1,
      11,
    ]);
    if (WebAssembly.validate(sourceWithSimd)) features.push('simd');
    if (sharedMemoryTest()) features.push('sharedMem');
  }
  try {
    new Worker(
      URL.createObjectURL(
        new Blob(["import('data:text/javascript,export default {}')"], { type: 'application/javascript' }),
      ),
    ).terminate();
    features.push('dynamicImportFromWorker');
  } catch {}
  return Object.freeze(features);
});

const hasMouse = memoize<boolean>(() => window.matchMedia('(hover: hover) and (pointer: fine)').matches);

export const reducedMotion: () => boolean = memoize<boolean>(
  () => window.matchMedia('(prefers-reduced-motion: reduce)').matches,
);

function sharedMemoryTest(): boolean {
  if (typeof Atomics !== 'object') return false;
  if (typeof SharedArrayBuffer !== 'function') return false;

  let mem;
  try {
    mem = new WebAssembly.Memory({ shared: true, initial: 1, maximum: 2 });
    if (!(mem.buffer instanceof SharedArrayBuffer)) return false;

    window.postMessage(mem.buffer, '*');
  } catch {
    return false;
  }
  return mem.buffer instanceof SharedArrayBuffer;
}

export function isVersionCompatible(version: string | undefined | false, vc?: VersionConstraint): boolean {
  if (!version) return false;
  if (!vc) return true;

  const v = split(version);

  if (vc.atLeast && isGreaterThan(split(vc.atLeast), v)) return false; // atLeast is an inclusive min

  return vc.below ? isGreaterThan(split(vc.below), v) : true; // below is an exclusive max

  function split(v: string): number[] {
    return v
      .split(/[._]/)
      .map(x => parseInt(x) || 0)
      .concat([0, 0, 0, 0]);
  }
  function isGreaterThan(left: number[], right: number[]): boolean {
    for (let i = 0; i < 4; i++)
      if (left[i] > right[i]) return true;
      else if (left[i] < right[i]) return false;
    return false;
  }
}
