export default function resizeHandle(el: HTMLElement) {
  el.addEventListener('mousedown', start => {

    const initialZoom = parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));
    let zoom = initialZoom;

    const saveZoom = window.lichess.debounce(() => {
      $.ajax({ method: 'post', url: '/pref/zoom?v=' + (100 + zoom) });
    }, 1000);

    const resize = (move: MouseEvent) => {

      const delta = move.clientX - start.clientX + move.clientY - start.clientY;

      zoom = Math.round(Math.min(100, Math.max(0, initialZoom + delta / 5)));

      document.body.setAttribute('style', '--zoom:' + zoom);
      window.lichess.dispatchEvent(window, 'resize');

      saveZoom();
    };

    document.body.classList.add('resizing');

    document.addEventListener('mousemove', resize);

    document.addEventListener('mouseup', () => {
      document.removeEventListener('mousemove', resize);
      document.body.classList.remove('resizing');
    }, { once: true });
  });
}
