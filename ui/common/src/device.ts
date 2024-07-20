import { Hooks } from 'snabbdom';
import { memoize } from './common';
import { bind } from './snabbdom';

const longPressDuration = 610;

export function bindMobileTapHold(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void): void {
  let longPressCountdown: number;

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

export const isMobile = (): boolean => isAndroid() || isIOS();

export const isAndroid = (): boolean => /Android/.test(navigator.userAgent);

export const isSafari = (): boolean => /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

export const isIOS = (constraint?: { below?: number; atLeast?: number }): boolean => {
  let answer = ios();
  if (!constraint || !answer) return answer;
  const version = parseFloat(navigator.userAgent.slice(navigator.userAgent.indexOf('Version/') + 8));
  if (constraint?.below) answer = version < constraint.below;
  if (answer && constraint?.atLeast) answer = version >= constraint.atLeast;
  return answer;
};

export const isChrome = (): boolean => /Chrome\//.test(navigator.userAgent);

export const isFirefox = (): boolean => /Firefox/.test(navigator.userAgent);

export const getFirefoxMajorVersion = (): number | undefined => {
  const match = /Firefox\/(\d*)/.exec(navigator.userAgent);
  return match && match.length > 1 ? parseInt(match[1]) : undefined;
};

export const isIOSChrome = (): boolean => /CriOS/.test(navigator.userAgent);

export const isTouchDevice = (): boolean => !hasMouse();

export const isIPad = (): boolean =>
  navigator?.maxTouchPoints > 2 && /iPad|Macintosh/.test(navigator.userAgent);

export type Feature = 'wasm' | 'sharedMem' | 'simd';

export const hasFeature = (feat?: string): boolean => !feat || features().includes(feat as Feature);

export const features: () => readonly Feature[] = memoize<readonly Feature[]>(() => {
  const features: Feature[] = [];
  if (
    typeof WebAssembly === 'object' &&
    typeof WebAssembly.validate === 'function' &&
    WebAssembly.validate(Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]))
  ) {
    features.push('wasm');
    if (sharedMemoryTest()) {
      features.push('sharedMem');
      // i32x4.dot_i16x8_s, i32x4.trunc_sat_f64x2_u_zero
      const sourceWithSimd = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0, 1, 12, 2, 96, 2, 123, 123, 1, 123, 96, 1, 123, 1, 123, 3, 3, 2, 0, 1, 7, 9, 2, 1, 97, 0, 0, 1, 98, 0, 1, 10, 19, 2, 9, 0, 32, 0, 32, 1, 253, 186, 1, 11, 7, 0, 32, 0, 253, 253, 1, 11]); // prettier-ignore
      if (WebAssembly.validate(sourceWithSimd)) features.push('simd');
    }
  }
  return Object.freeze(features);
});

const ios = memoize<boolean>(() => /iPhone|iPod/.test(navigator.userAgent) || isIPad());

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
  } catch (_) {
    return false;
  }
  return mem.buffer instanceof SharedArrayBuffer;
}
