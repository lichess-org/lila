import throttle from './throttle';

let lastMainBoardHeight: number | undefined;

export function runner(hacks: () => void, throttleMs: number = 100) {

  let timeout: number | undefined;

  const runHacks = throttle(throttleMs, () => {
    window.lichess.raf(() => {
      hacks();
      schedule();
    });
  });

  function schedule() {
    timeout && clearTimeout(timeout);
    timeout = setTimeout(runHacks, 500);
  }

  runHacks();
}

// Firefox 60- needs this to properly compute the grid layout.
export function fixMainBoardHeight(container: HTMLElement) {
  const el = container.querySelector('.main-board') as HTMLElement,
    width = el.offsetWidth;
  if (lastMainBoardHeight != width) {
    lastMainBoardHeight = width;
    el.style.height = width + 'px';
    window.lichess.dispatchEvent(document.body, 'chessground.resize');
  }
}
