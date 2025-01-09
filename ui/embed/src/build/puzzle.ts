import { usiToSquareNames } from 'shogiops/compat';
import { resize } from '../util';

window.lishogi.ready.then(() => {
  const el = document.querySelector<HTMLAnchorElement>('#daily-puzzle')!,
    board = el.querySelector<HTMLElement>('.mini-board')!,
    lm = board.getAttribute('data-lastmove'),
    sfen = board.getAttribute('data-sfen')!,
    splitSfen = sfen.split(' ');

  el.target = '_blank';

  window.Shogiground(
    {
      coordinates: { enabled: false },
      drawable: { enabled: false, visible: false },
      viewOnly: true,
      sfen: { board: splitSfen[0], hands: splitSfen[2] },
      hands: { inlined: true },
      lastDests: lm ? usiToSquareNames(lm) : undefined,
      orientation: board.getAttribute('data-color') as Color,
    },
    { board: board.firstElementChild as HTMLElement },
  );

  resize(el);
  window.addEventListener('resize', () => resize(el));
});
