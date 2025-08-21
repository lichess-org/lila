import { isTouchDevice } from './device';

// pointer events that don't interfere with vertical scrolling on touch devices

export type PointerListeners = {
  click?: (e: PointerEvent) => any;
  hold?: 'click' | ((e: PointerEvent) => any);
  hscrub?: (dx: number | undefined, e: PointerEvent) => any;
  holdDuration?: number; // default 500
  scrubInterval?: number; // default 100
};

export function addPointerListeners(el: HTMLElement, listeners: PointerListeners): void {
  const { click, hold, hscrub } = listeners;
  const g = { timer: 0, x: 0, y: 0, lastMove: 0 };
  const holdDuration = listeners.holdDuration ?? 500;
  const scrubInterval = listeners.scrubInterval ?? 100;

  const reset = (e: PointerEvent) => {
    clearTimeout(g.timer);
    if (g.lastMove) hscrub?.(undefined, e);
    g.x = g.y = g.timer = g.lastMove = 0;
    if (e?.type === 'pointerdown') return;
    else if (e?.type === 'pointerup') e.preventDefault();
    el.removeEventListener('pointerup', pointerup);
    el.removeEventListener('pointermove', pointermove);
    el.removeEventListener('pointercancel', pointercancel);
  };

  const pointerdown = (e: PointerEvent) => {
    [g.x, g.y] = [e.clientX, e.clientY];
    g.timer = window.setTimeout(() => {
      if (!hold) return;
      if (hold === 'click') click?.(e);
      else hold(e);
      reset(e);
    }, holdDuration);
    el.addEventListener('pointerup', pointerup, { passive: false, once: true });
    el.addEventListener('pointermove', pointermove, { passive: false });
    el.addEventListener('pointercancel', pointercancel, { passive: true, once: true });
  };

  const pointermove = (e: PointerEvent) => {
    const [dx, dy] = [e.clientX - g.x, e.clientY - g.y];

    if (!g.lastMove && Math.abs(dy) > 6) return reset(e);
    if (!hscrub) return;

    e.preventDefault();
    clearTimeout(g.timer);
    g.timer = 0;

    if (dx && performance.now() - g.lastMove > scrubInterval) {
      g.lastMove = performance.now();
      if (hscrub(dx, e)) reset(e);
      else [g.x, g.y] = [e.clientX, e.clientY];
    }
  };

  const pointerup = (e: PointerEvent) => {
    if (g.timer && click) {
      e.preventDefault();
      click(e);
    }
    console.log('yahtzee!');
    reset(e);
  };

  const pointercancel = reset;

  el.addEventListener('pointerdown', pointerdown, { passive: true });

  if (isTouchDevice() && (hold || hscrub)) {
    el.addEventListener('contextmenu', e => e.preventDefault(), { passive: false });
  }
}
