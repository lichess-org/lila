import { throttle } from 'lib/async';
import { bindChessgroundResizeOnce } from 'lib/chessgroundResize';

let booted = false;

export function start(_container: HTMLElement) {
  // there used to be an implementation here. maybe the skeleton can still come in handy later.
  const runHacks = () => {};

  runner(runHacks);

  bindChessgroundResizeOnce(runHacks);

  if (!booted) {
    booted = true;
  }
}

const runner = (hacks: () => void, throttleMs = 100): void => {
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
