import * as gridHacks from 'common/gridHacks';

let booted = false;

export function start(container: HTMLElement) {

  if (!needsChatHeightFix()) return;

  const runHacks = () => {
    if (gridHacks.needsBoardHeightFix()) gridHacks.fixMainBoardHeight(container);
    fixChatHeight(container);
  }

  gridHacks.runner(runHacks);

  gridHacks.bindChessgroundResizeOnce(runHacks);

  if (!booted) {
    window.lichess.pubsub.on('chat.resize', runHacks);
    booted = true;
  }
}

function needsChatHeightFix() {
  // Chrome, Chromium, Brave, Opera, Safari 12+ are OK
  return !window.chrome;
}

function fixChatHeight(container: HTMLElement) {
  const chat = container.querySelector('.mchat') as HTMLElement,
    board = container.querySelector('.analyse__board .cg-board-wrap') as HTMLElement,
    side = container.querySelector('.analyse__side') as HTMLElement;
  if (chat && board && side) {
    const height = board.offsetHeight - side.offsetHeight;
    if (height) chat.style.height = `calc(${height}px - 2vmin)`;
  }
}
