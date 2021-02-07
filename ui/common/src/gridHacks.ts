import throttle from './throttle';

export const runner = (hacks: () => void, throttleMs: number = 100): void => {
  let timeout: number | undefined;

  const runHacks = throttle(throttleMs, () =>
    requestAnimationFrame(() => {
      hacks();
      schedule();
    })
  );

  function schedule() {
    timeout && clearTimeout(timeout);
    timeout = setTimeout(runHacks, 500);
  }

  runHacks();
};

let boundChessgroundResize = false;

export const bindChessgroundResizeOnce = (f: () => void): void => {
  if (!boundChessgroundResize) {
    boundChessgroundResize = true;
    document.body.addEventListener('chessground.resize', f);
  }
};
