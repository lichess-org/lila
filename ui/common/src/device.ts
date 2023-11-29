import { memoize } from './common';
import { bind } from './snabbdom';
import { domDialog } from './dialog';

const longPressDuration = 610;

export function bindMobileTapHold(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void) {
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

export const hookMobileMousedown = (f: (e: Event) => any) =>
  bind('ontouchstart' in window ? 'click' : 'mousedown', f);

export const isMobile = (): boolean => isAndroid() || isIOS();

export const isAndroid = (): boolean => /Android/.test(navigator.userAgent);

export const isIOS = (constraint?: { below?: number; atLeast?: number }) => {
  let answer = ios();
  if (!constraint || !answer) return answer;
  const version = parseFloat(navigator.userAgent.slice(navigator.userAgent.indexOf('Version/') + 8));
  if (constraint?.below) answer = version < constraint.below;
  if (answer && constraint?.atLeast) answer = version >= constraint.atLeast;
  return answer;
};

export const isChrome = (): boolean => /Chrome\//.test(navigator.userAgent);

export const isFirefox = (): boolean => /Firefox/.test(navigator.userAgent);

export const getFirefoxMajorVersion = (): number => {
  if (!isFirefox()) {
    return 0;
  }
  const match = /Firefox\/(\d*)/.exec(navigator.userAgent);
  if (!match || match.length < 2) return 0;
  return parseInt(match[1]);
};

export const isIOSChrome = (): boolean => /CriOS/.test(navigator.userAgent);

export const isTouchDevice = () => !hasMouse();

export const isIPad = (): boolean =>
  navigator?.maxTouchPoints > 2 && /iPad|Macintosh/.test(navigator.userAgent);

export type Feature = 'wasm' | 'sharedMem' | 'simd' | 'webWorkerDynamicImport';

export const hasFeature = (feat?: string) => !feat || features().includes(feat as Feature);

export const features = memoize<readonly Feature[]>(() => {
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
  if (!getFirefoxMajorVersion() || getFirefoxMajorVersion() >= 114) {
    features.push('webWorkerDynamicImport');
  }
  return Object.freeze(features);
});

export async function showDiagnostic() {
  const logs = await lichess.log.get();
  const text =
    `User Agent: ${navigator.userAgent}\n` +
    `Cores: ${navigator.hardwareConcurrency}\n` +
    `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}\n` +
    `Screen: ${window.screen.width}x${window.screen.height}\n` +
    `Device Pixel Ratio: ${window.devicePixelRatio}\n` +
    `Language: ${navigator.language}` +
    (logs ? `\n\n${logs}` : '');

  const dlg = await domDialog({
    class: 'diagnostic',
    htmlText:
      `<h2>Diagnostics</h2><pre tabindex="0" class="err">${lichess.escapeHtml(text)}</pre>` +
      (logs ? `<button class="clear button">Clear Logs</button>` : ''),
  });
  const select = () =>
    setTimeout(() => {
      const range = document.createRange();
      range.selectNodeContents(dlg.view.querySelector('.err')!);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
    }, 0);
  dlg.view.querySelector('.err')?.addEventListener('focus', select);
  dlg.view.querySelector('.clear')?.addEventListener('click', () => lichess.log.clear().then(lichess.reload));
  dlg.showModal();
}

const ios = memoize<boolean>(() => /iPhone|iPod/.test(navigator.userAgent) || isIPad());

const hasMouse = memoize<boolean>(() => window.matchMedia('(hover: hover) and (pointer: fine)').matches);

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
