import * as sg from 'shogiground/types';

export type MouchEvent = MouseEvent & TouchEvent;

export default function resizeHandle(
  els: sg.BoardElements,
  pref: number,
  conf: {
    ply?: number;
    initialPly?: number;
    visible?: () => boolean;
  }
) {
  if (!pref) return;

  const el = document.createElement('sg-resize');
  els.board.appendChild(el);

  const startResize = (start: MouchEvent) => {
    start.preventDefault();

    const mousemoveEvent = start.type === 'touchstart' ? 'touchmove' : 'mousemove';
    const mouseupEvent = start.type === 'touchstart' ? 'touchend' : 'mouseup';

    const startPos = eventPosition(start)!;
    const initialZoom = parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));
    let zoom = initialZoom;

    const saveZoom = window.lishogi.debounce(() => {
      $.ajax({ method: 'post', url: '/pref/zoom?v=' + (100 + zoom) });
    }, 700);

    const resize = (move: MouchEvent) => {
      const pos = eventPosition(move)!;
      const delta = pos[0] - startPos[0] + pos[1] - startPos[1];

      zoom = Math.round(Math.min(100, Math.max(0, initialZoom + delta / 10)));

      document.body.style.setProperty('--zoom', zoom.toString());
      window.lishogi.dispatchEvent(window, 'resize');

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
    const toggle = (ply: number) =>
      el.classList.toggle('none', conf.visible ? !conf.visible() : ply - (conf.initialPly || 0) >= 2);
    toggle(conf.ply || 0);
    window.lishogi.pubsub.on('ply', toggle);
  }

  addNag(el);
}

function eventPosition(e: MouchEvent): [number, number] | undefined {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}

function addNag(el: HTMLElement) {
  const storage = window.lishogi.storage.makeBoolean('resize-nag');
  if (storage.get()) return;

  window.lishogi.loadCssPath('nag-circle');
  el.title = 'Drag to resize';
  el.innerHTML = '<div class="nag-circle"></div>';
  for (const mousedownEvent of ['touchstart', 'mousedown']) {
    el.addEventListener(
      mousedownEvent,
      () => {
        storage.set(true);
        el.innerHTML = '';
      },
      { once: true }
    );
  }

  setTimeout(() => storage.set(true), 15000);
}
