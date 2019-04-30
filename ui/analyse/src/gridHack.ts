import throttle from 'common/throttle';

let timeout;
let booted = false;

export function start(container: HTMLElement) {

  /* Detected browsers with a correct grid min-content implementation:
   * Chrome, Chromium, Brave, Opera
   */
  if (window.chrome) return;

  const resize = throttle(100, () => {
    window.requestAnimationFrame(() => {
      const chat = container.querySelector('.mchat') as HTMLElement,
      board = container.querySelector('.analyse__board') as HTMLElement,
      side = container.querySelector('.analyse__side') as HTMLElement;
      if (chat && board && side) {
        const height = board.offsetHeight - side.offsetHeight;
        if (height) chat.style.height = `calc(${height}px - 2vmin)`;
      }
      schedule();
    });
  });

  function schedule() {
    timeout && clearTimeout(timeout);
    timeout = setTimeout(resize, 500);
  }

  resize();

  if (!booted) {
    booted = true;
    document.body.addEventListener('chessground.resize', resize);
    window.lichess.pubsub.on('analyse.grid-hack', resize);
  }
}
