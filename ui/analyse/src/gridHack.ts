import throttle from 'common/throttle';

let timeout;
let booted = false;

export function start(container: HTMLElement) {

  const resize = throttle(500, () => {
    window.requestAnimationFrame(() => {
      const chat = container.querySelector('.mchat') as HTMLElement;
      const board = container.querySelector('.analyse__board') as HTMLElement;
      const side = container.querySelector('.analyse__side') as HTMLElement;
      if (!chat || !board || !side) return;
      const height = board.offsetHeight - side.offsetHeight;
      if (height) chat.style.height = `calc(${height}px - 2vmin)`;
      schedule();
    });
  });

  function schedule() {
    timeout && clearTimeout(timeout);
    timeout = setTimeout(resize, 1000);
  }

  resize();

  if (!booted) {
    booted = true;
    document.body.addEventListener('chessground.resize', resize);
    window.lichess.pubsub.on('analyse.grid-hack', resize);
  }
}
