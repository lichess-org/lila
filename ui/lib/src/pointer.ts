import { isTouchDevice } from './device';

// PointerEvent listeners to allow vertical scrolling on touch devices

export type PointerListeners = {
  click?: (e: PointerEvent) => any;
  hold?: 'click' | ((e: PointerEvent) => any);
  hscrub?: (dx: number | 'pointerup', e: PointerEvent) => any; // truthy return cancels
  holdDuration?: number;
  scrubInterval?: number;
};

export function addPointerListeners(el: HTMLElement, listeners: PointerListeners): void {
  const { click, hold, hscrub } = listeners;
  const g = { timer: 0, x: 0, y: 0, lastMove: 0 };
  const holdDuration = listeners.holdDuration ?? 500;
  const scrubInterval = listeners.scrubInterval ?? 100;

  const reset = (e: PointerEvent) => {
    clearTimeout(g.timer);
    if (g.lastMove) hscrub?.('pointerup', e);
    el.releasePointerCapture(e.pointerId);
    el.removeEventListener('pointermove', pointermove);
    g.x = g.y = g.timer = g.lastMove = 0;
  };

  const pointerdown = (e: PointerEvent) => {
    [g.x, g.y] = [e.clientX, e.clientY];
    g.timer = window.setTimeout(() => {
      if (!hold) return;
      if (hold === 'click') click?.(e);
      else hold(e);
      reset(e);
    }, holdDuration);
    el.addEventListener('pointermove', pointermove, { passive: false });
  };

  const pointermove = (e: PointerEvent) => {
    const [dx, dy] = [e.clientX - g.x, e.clientY - g.y];

    if (!g.lastMove && Math.abs(dy) > 12) return reset(e);
    if (!hscrub || Math.abs(dx) < 5 || Math.abs(dx) < Math.abs(dy) * 2) return;
    clearTimeout(g.timer);
    g.timer = 0;

    if (dx && performance.now() - g.lastMove > scrubInterval) {
      if (!el.hasPointerCapture(e.pointerId)) el.setPointerCapture(e.pointerId);
      e.preventDefault();
      g.lastMove = performance.now();
      if (hscrub(dx, e)) reset(e);
      else [g.x, g.y] = [e.clientX, e.clientY];
    }
  };

  const pointerup = (e: PointerEvent) => {
    if (g.timer && click) click(e);
    reset(e);
    e.preventDefault();
  };

  el.addEventListener('pointerup', pointerup, { passive: false });
  el.addEventListener('pointerdown', pointerdown, { passive: true });
  el.addEventListener('pointercancel', reset, { passive: true });

  if (isTouchDevice() && (hold || hscrub)) {
    el.addEventListener('contextmenu', e => e.preventDefault(), { passive: false });
  }
}
