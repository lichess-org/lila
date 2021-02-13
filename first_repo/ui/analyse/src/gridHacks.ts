import * as gridHacks from 'common/gridHacks';

let booted = false;

export function start(container: HTMLElement) {
  // Chrome, Chromium, Brave, Opera, Safari 12+ are OK
  if (window.chrome) return;

  const runHacks = () => fixChatHeight(container);

  gridHacks.runner(runHacks);

  gridHacks.bindChessgroundResizeOnce(runHacks);

  if (!booted) {
    lichess.pubsub.on('chat.resize', runHacks);
    booted = true;
  }
}

function fixChatHeight(container: HTMLElement) {
  const chat = container.querySelector('.mchat') as HTMLElement,
    board = container.querySelector('.analyse__board .cg-wrap') as HTMLElement,
    side = container.querySelector('.analyse__side') as HTMLElement;
  if (chat && board && side) {
    const height = board.offsetHeight - side.offsetHeight;
    if (height) chat.style.height = `calc(${height}px - 2vmin)`;
  }
}
