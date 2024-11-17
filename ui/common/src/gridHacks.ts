import { bindChessgroundResize } from './resize';
import { throttle } from './timing';

export const runner = (hacks: () => void, throttleMs = 100): void => {
  let timeout: Timeout | undefined;

  const runHacks = throttle(throttleMs, () =>
    requestAnimationFrame(() => {
      hacks();
      schedule();
    }),
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
    bindChessgroundResize(f);
  }
};
