import * as cg from 'chessground/types';
import * as xhr from './xhr';
import debounce from './debounce';

export type MouchEvent = MouseEvent & TouchEvent;

type Visible = (ply: Ply) => boolean;

export default function resizeHandle(els: cg.Elements, pref: number, ply: number, visible?: Visible) {
  if (!pref) return;

  const el = document.createElement('cg-resize');
  els.container.appendChild(el);

  const startResize = (start: MouchEvent) => {
    start.preventDefault();

    const mousemoveEvent = start.type === 'touchstart' ? 'touchmove' : 'mousemove',
      mouseupEvent = start.type === 'touchstart' ? 'touchend' : 'mouseup',
      startPos = eventPosition(start)!,
      initialZoom = parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));
    let zoom = initialZoom;

    const saveZoom = debounce(() => xhr.text(`/pref/zoom?v=${100 + zoom}`, { method: 'post' }), 700);

    const resize = (move: MouchEvent) => {
      const pos = eventPosition(move)!,
        delta = pos[0] - startPos[0] + pos[1] - startPos[1];

      zoom = Math.round(Math.min(100, Math.max(0, initialZoom + delta / 10)));

      document.body.setAttribute('style', '--zoom:' + zoom);
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
      { once: true }
    );
  };

  el.addEventListener('touchstart', startResize, { passive: false });
  el.addEventListener('mousedown', startResize, { passive: false });

  if (pref == 1) {
    const toggle = (ply: number) => el.classList.toggle('none', visible ? !visible(ply) : ply >= 2);
    toggle(ply);
    lichess.pubsub.on('ply', toggle);
  }
}

function eventPosition(e: MouchEvent): [number, number] | undefined {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}
