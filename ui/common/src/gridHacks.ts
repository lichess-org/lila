import throttle from './throttle';

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

let lastMainBoardHeight: number | undefined;

// Firefox 60- needs this to properly compute the grid layout.
export function fixMainBoardHeight(container: HTMLElement) {
  const mainBoard = container.querySelector('.main-board') as HTMLElement,
    width = mainBoard.offsetWidth;
  if (lastMainBoardHeight != width) {
    lastMainBoardHeight = width;
    mainBoard.style.height = width + 'px';
    (mainBoard.querySelector('.cg-wrap') as HTMLElement).style.height = width + 'px';
    window.lichess.dispatchEvent(document.body, 'chessground.resize');
  }
}

let boundChessgroundResize = false;

export function bindChessgroundResizeOnce(f: () => void) {
  if (!boundChessgroundResize) {
    boundChessgroundResize = true;
    document.body.addEventListener('chessground.resize', f);
  }
}

export function needsBoardHeightFix() {
  // Chrome, Chromium, Brave, Opera, Safari 12+ are OK
  if (window.chrome) return false;

  // Firefox >= 61 is OK
  const ffv = navigator.userAgent.split('Firefox/');
  return !ffv[1] || parseInt(ffv[1]) < 61;
}
