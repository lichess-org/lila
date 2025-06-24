import { throttle } from 'lib/async';
import { bindChessgroundResizeOnce } from 'lib/chessgroundResize';
import { pubsub } from 'lib/pubsub';

let booted = false;

export function start(container: HTMLElement) {
  const runHacks = () => fixChatHeight(container);

  runner(runHacks);

  bindChessgroundResizeOnce(runHacks);

  if (!booted) {
    pubsub.on('chat.resize', runHacks);
    booted = true;
  }
}

function fixChatHeight(container: HTMLElement) {
  const chat = container.querySelector('.mchat') as HTMLElement,
    board = container.querySelector('.analyse__board .cg-wrap') as HTMLElement,
    side = container.querySelector('.analyse__side') as HTMLElement;
  if (chat && board && side && !chat.nextElementSibling?.classList.contains('vertical-resize')) {
    const height = board.offsetHeight - side.offsetHeight;
    if (height) chat.style.height = `calc(${height}px - 2vmin)`;
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
