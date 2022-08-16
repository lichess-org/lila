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

  el.addEventListener('touchmove', () => {
    clearTimeout(longPressCountdown);
  });

  el.addEventListener('touchcancel', () => {
    clearTimeout(longPressCountdown);
  });

  el.addEventListener('touchend', () => {
    clearTimeout(longPressCountdown);
  });
}

export function bindMobileMousedown(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void): void {
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
}

export function hookMobileMousedown(f: (e: Event) => any) {
  return bind('ontouchstart' in window ? 'click' : 'mousedown', f);
}

export const isAndroid = (): boolean => {
  return /Android/.test(navigator.platform);
};

export const isIOS = (): boolean => {
  return /iPad|iPhone|iPod/.test(navigator.platform) ? true : isIPad();
};

export const isIPad = (): boolean => {
  return navigator?.maxTouchPoints > 2 && /MacIntel/.test(navigator.platform);
};
