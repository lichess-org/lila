import type { Elements } from '@lichess-org/chessground/types';

import { debounce } from './async';
import { ShowResizeHandle } from './prefs';
import { pubsub } from './pubsub';
import * as xhr from './xhr';

type MouchEvent = Event & Partial<MouseEvent & TouchEvent>;

type Visible = (ply: Ply) => boolean;

export function applyBoardSettings(): void {
  const filters: string[] = [];
  const styles = window.getComputedStyle(document.body);
  const brightness = Number(styles.getPropertyValue('---board-brightness'));
  const contrast = Number(styles.getPropertyValue('---board-contrast'));
  const hue = Number(styles.getPropertyValue('---board-hue'));
  const isCustomBrightness = !isNaN(brightness) && brightness !== 100;

  if (isCustomBrightness) filters.push(`brightness(calc(var(---board-brightness) / 100))`);
  if (!isNaN(contrast) && contrast !== 100) filters.push(`contrast(calc(var(---board-contrast) / 100))`);
  if (!isNaN(hue) && hue !== 0) filters.push(`hue-rotate(calc(var(---board-hue) * 3.6deg))`);
  document.body.style.setProperty('---board-filter', filters.join(' ') || 'none');
  document.body.classList.toggle('custom-brightness', isCustomBrightness);
}

export default function resizeHandle(
  els: Elements,
  pref: ShowResizeHandle,
  ply: number,
  visible?: Visible,
): void {
  if (pref === ShowResizeHandle.Never) return;

  const el = document.createElement('cg-resize');
  els.container.appendChild(el);

  const startResize = (start: MouchEvent) => {
    start.preventDefault();

    const mousemoveEvent = start.type === 'touchstart' ? 'touchmove' : 'mousemove',
      mouseupEvent = start.type === 'touchstart' ? 'touchend' : 'mouseup',
      startPos = eventPosition(start)!,
      initialZoom = parseInt(window.getComputedStyle(document.body).getPropertyValue('---zoom'));
    let zoom = initialZoom;

    const saveZoom = debounce(() => xhr.text(`/pref/zoom?v=${zoom}`, { method: 'post' }), 700);

    const resize = (move: MouchEvent) => {
      const pos = eventPosition(move)!,
        delta = pos[0] - startPos[0] + pos[1] - startPos[1];

      zoom = Math.round(Math.min(100, Math.max(0, initialZoom + delta / 10)));

      document.body.style.setProperty('---zoom', zoom.toString());
      window.dispatchEvent(new Event('resize'));

      saveZoom();
    };

    document.body.classList.add('resizing');

    document.addEventListener(mousemoveEvent, resize);

    document.addEventListener(
      mouseupEvent,
      () => {
        document.removeEventListener(mousemoveEvent, resize);
        document.body.classList.remove('resizing');
      },
      { once: true },
    );
  };

  el.addEventListener('touchstart', startResize, { passive: false });
  el.addEventListener('mousedown', startResize, { passive: false });

  if (pref === ShowResizeHandle.OnlyAtStart) {
    const toggle = (ply: number) => el.classList.toggle('none', visible ? !visible(ply) : ply >= 2);
    toggle(ply);
    pubsub.on('ply', toggle);
  }
}

function eventPosition(e: MouchEvent) {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY!];
  if (e.targetTouches?.[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}
