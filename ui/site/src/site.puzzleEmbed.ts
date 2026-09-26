import { uciToMove } from '@lichess-org/chessground/util';

import { embedChessground } from './asset';

// https://lichess.org/training/frame
window.onload = async () => {
  const el = document.querySelector<HTMLElement>('#daily-puzzle');
  const board = el?.querySelector<HTMLAnchorElement>('.mini-board');

  if (!el || !board) return;

  const [fen, orientation, lm] = board.getAttribute('data-state')?.split(',') ?? [];

  (await embedChessground()).Chessground(board.firstChild, {
    coordinates: false,
    drawable: { enabled: false, visible: false },
    viewOnly: true,
    fen,
    lastMove: uciToMove(lm),
    orientation: orientation as 'white' | 'black',
  });

  const resize = () => {
    const windowHeight = window.innerHeight;
    if (el.offsetHeight > windowHeight) {
      const textHeightOffset = el.querySelector<HTMLElement>('span.text')?.offsetHeight ?? 0;
      el.style.maxWidth = windowHeight - textHeightOffset + 'px';
    }
  };
  resize();
  window.addEventListener('resize', resize);
};
