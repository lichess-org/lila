import throttle from './throttle';

export function runner(hacks: () => void, throttleMs: number = 100) {

  let timeout: number | undefined;

  const runHacks = throttle(throttleMs, () => {
    window.lidraughts.raf(() => {
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
    window.lidraughts.dispatchEvent(document.body, 'draughtsground.resize');
  }
}

let boundDraughtsgroundResize = false;

export function bindDraughtsgroundResizeOnce(f: () => void) {
  if (!boundDraughtsgroundResize) {
    boundDraughtsgroundResize = true;
    document.body.addEventListener('draughtsground.resize', f);
  }
}

export function needsBoardHeightFix() {
  // Chrome, Chromium, Brave, Opera, Safari 12+ are OK
  if (window.chrome) return false;

  // Firefox >= 61 is OK
  const ffv = navigator.userAgent.split('Firefox/');
  return !ffv[1] || parseInt(ffv[1]) < 61;
}
