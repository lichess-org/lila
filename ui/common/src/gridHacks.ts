import throttle from './throttle';

export const runner = (hacks: () => void, throttleMs: number = 100): void => {

  let timeout: number | undefined;

  const runHacks = throttle(throttleMs, () => {
    requestAnimationFrame(() => {
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
export const fixMainBoardHeight = (container: HTMLElement): void => {
  const mainBoard = container.querySelector('.main-board') as HTMLElement;
  if (mainBoard) {
    const width = mainBoard.offsetWidth;
    if (lastMainBoardHeight != width) {
      lastMainBoardHeight = width;
      mainBoard.style.height = width + 'px';
      (mainBoard.querySelector('.cg-wrap') as HTMLElement).style.height = width + 'px';
      document.body.dispatchEvent(new Event('chessground.resize'));
    }
  }
}

let boundChessgroundResize = false;

export const bindChessgroundResizeOnce = (f: () => void): void => {
  if (!boundChessgroundResize) {
    boundChessgroundResize = true;
    document.body.addEventListener('chessground.resize', f);
  }
}

export const needsBoardHeightFix = (): boolean => {
  // Chrome, Chromium, Brave, Opera, Safari 12+ are OK
  if (window.chrome) return false;

  // Firefox >= 61 is OK
  const ffv = navigator.userAgent.split('Firefox/');
  return !ffv[1] || parseInt(ffv[1]) < 61;
}
