import { isTouchDevice } from './device';

// PointerEvent listeners to allow vertical scrolling on touch devices

export type PointerListeners = {
  click?: (e: PointerEvent) => void;
  hold?: 'click' | ((e: PointerEvent) => void);
  holdDuration?: number;
};

export function addPointerListeners(el: HTMLElement, listeners: PointerListeners): void {
  const { click, hold } = listeners;
  const g = { timer: 0, y: 0 };
  const holdDuration = listeners.holdDuration ?? 500;

  const reset = (e: PointerEvent) => {
    clearTimeout(g.timer);
    el.releasePointerCapture(e.pointerId);
    el.removeEventListener('pointermove', pointermove);
    g.y = g.timer = 0;
  };

  const pointerdown = (e: PointerEvent) => {
    g.y = e.clientY;
    g.timer = window.setTimeout(() => {
      if (!hold) return;
      if (hold === 'click') click?.(e);
      else hold(e);
      reset(e);
    }, holdDuration);
    el.addEventListener('pointermove', pointermove, { passive: false });
  };

  const pointermove = (e: PointerEvent) => {
    const dy = e.clientY - g.y;
    if (Math.abs(dy) > 12) return reset(e); // page scroll
  };

  const pointerup = (e: PointerEvent) => {
    if (g.timer && click) click(e);
    reset(e);
    e.preventDefault();
  };

  el.addEventListener('pointerup', pointerup, { passive: false });
  el.addEventListener('pointerdown', pointerdown, { passive: true });
  el.addEventListener('pointercancel', reset, { passive: true });

  if (isTouchDevice() && hold) {
    el.addEventListener('contextmenu', e => e.preventDefault(), { passive: false });
  }
}
