import * as cg from 'chessground/types';

export type MouchEvent = MouseEvent & TouchEvent;

type Visible = (ply: Ply) => boolean;

export default function resizeHandle(els: cg.Elements, pref: number, ply: number, visible?: Visible) {

  if (!pref) return;

  const el = document.createElement('cg-resize');
  els.container.appendChild(el);

  const mousemoveEvent = window.lichess.hasTouchEvents ? 'touchmove' : 'mousemove';
  const mouseupEvent = window.lichess.hasTouchEvents ? 'touchend' : 'mouseup';

  el.addEventListener(window.lichess.mousedownEvent, (start: MouchEvent) => {

    start.preventDefault();

    const startPos = eventPosition(start)!;
    const initialZoom = parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));
    let zoom = initialZoom;

    const saveZoom = window.lichess.debounce(() => {
      $.ajax({ method: 'post', url: '/pref/zoom?v=' + (100 + zoom) });
    }, 700);

    const resize = (move: MouchEvent) => {

      const pos = eventPosition(move)!;
      const delta = pos[0] - startPos[0] + pos[1] - startPos[1];

      zoom = Math.round(Math.min(100, Math.max(0, initialZoom + delta / 10)));

      document.body.setAttribute('style', '--zoom:' + zoom);
      window.lichess.dispatchEvent(window, 'resize');

      saveZoom();
    };

    document.body.classList.add('resizing');

    document.addEventListener(mousemoveEvent, resize);

    document.addEventListener(mouseupEvent, () => {
      document.removeEventListener(mousemoveEvent, resize);
      document.body.classList.remove('resizing');
    }, { once: true });
  });

  if (pref == 1) {
    const toggle = (ply: number) => el.classList.toggle('none', visible ? !visible(ply) : ply >= 2);
    toggle(ply);
    window.lichess.pubsub.on('ply', toggle);
  }

  addNag(el);
}

function eventPosition(e: MouchEvent): [number, number] | undefined {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}

function addNag(el: HTMLElement) {

  const storage = window.lichess.storage.makeBoolean('resize-nag');
  if (storage.get()) return;

  window.lichess.loadCssPath('nag-circle');
  el.title = 'Drag to resize';
  el.innerHTML = '<div class="nag-circle"></div>';
  el.addEventListener(window.lichess.mousedownEvent, () => {
    storage.set(true);
    el.innerHTML = '';
  }, { once: true });

  setTimeout(() => storage.set(true), 15000);
}
