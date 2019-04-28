export type MouchEvent = MouseEvent & TouchEvent;

export default function resizeHandle(el: HTMLElement) {

  const mousemoveEvent = window.lichess.hasTouchEvents ? 'touchmove' : 'mousedown';
  const mouseupEvent = window.lichess.hasTouchEvents ? 'touchend' : 'mouseup';

  el.addEventListener(window.lichess.mousedownEvent, (start: MouchEvent) => {

    start.preventDefault();

    const startPos = eventPosition(start)!;
    const initialZoom = parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));
    let zoom = initialZoom;

    const saveZoom = window.lichess.debounce(() => {
      $.ajax({ method: 'post', url: '/pref/zoom?v=' + (100 + zoom) });
    }, 1000);

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
}

function eventPosition(e: MouchEvent): [number, number] | undefined {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}
