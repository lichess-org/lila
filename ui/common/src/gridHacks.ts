import { throttlePromise, finallyDelay } from './throttle';

export const runner = (hacks: () => void, throttleMs = 100): void => {
  let timeout: number | undefined;

  const runHacks = throttlePromise(
    finallyDelay(throttleMs, () =>
      requestAnimationFrame(() => {
        hacks();
        schedule();
      })
    )
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
