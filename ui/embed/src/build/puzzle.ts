import { usiToSquareNames } from 'shogiops/compat';
import { resize } from '../util';

window.lishogi.ready.then(() => {
  const el = document.querySelector<HTMLAnchorElement>('#daily-puzzle')!;
  const board = el.querySelector<HTMLElement>('.mini-board')!;
  const lm = board.getAttribute('data-lastmove');
  const sfen = board.getAttribute('data-sfen')!;
  const splitSfen = sfen.split(' ');

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
