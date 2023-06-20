import { memoize } from './common';
import { bind } from './snabbdom';

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
        { passive: false }
      );
    }
  };

export const hookMobileMousedown = (f: (e: Event) => any) =>
  bind('ontouchstart' in window ? 'click' : 'mousedown', f);

export const isMobile = (): boolean => isAndroid() || isIOS();

export const isAndroid = (): boolean => /Android/.test(navigator.userAgent);

export const isIOS = (): boolean => /iPhone|iPod/.test(navigator.userAgent) || isIPad();

// some newer iPads pretend to be Macs, hence checking for "Macintosh"
export const isIPad = (): boolean =>
  navigator?.maxTouchPoints > 2 && /iPad|Macintosh/.test(navigator.userAgent);

const hasMouse = memoize<boolean>(() => window.matchMedia('(hover: hover) and (pointer: fine)').matches);

export const isTouchDevice = () => !hasMouse();
