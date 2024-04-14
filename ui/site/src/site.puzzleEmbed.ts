import { uciToMove } from 'chessground/util';
import { embedChessground } from './asset';

// https://lichess.org/training/frame
window.onload = async () => {
  const el = document.querySelector('#daily-puzzle') as HTMLElement,
    board = el.querySelector('.mini-board') as HTMLAnchorElement,
    [fen, orientation, lm] = board.getAttribute('data-state')!.split(',');

  (await embedChessground()).Chessground(board.firstChild as HTMLElement, {
    coordinates: false,
    drawable: { enabled: false, visible: false },
    viewOnly: true,
    fen: fen,
    lastMove: uciToMove(lm),
    orientation: orientation as 'white' | 'black',
  });

  const resize = () => {
    if (el.offsetHeight > window.innerHeight)
      el.style.maxWidth =
        window.innerHeight - (el.querySelector('span.text') as HTMLElement).offsetHeight + 'px';
  };
  resize();
  window.addEventListener('resize', resize);
};
