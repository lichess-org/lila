import Lpv from '@lichess-org/pgn-viewer';
import type { Opening } from './interfaces';
import { set } from 'lib/data';

const numberFormat = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat() : null;
export const formatNumber = (n: number): string => (numberFormat ? numberFormat.format(n) : '' + n);

interface AnimateNumber {
  duration: number;
  render: (n: number) => string;
}
export const animateNumber = (counter: HTMLElement, opts: Partial<AnimateNumber>): void => {
  const o: AnimateNumber = { ...{ duration: 1500, render: formatNumber }, ...opts };
  const value = parseInt(counter.dataset['value'] || '0');
  const startAt = performance.now();
  const stopAt = startAt + o.duration;
  const animate = () => {
    const now = performance.now();
    if (now >= stopAt) counter.innerHTML = o.render(value);
    else {
      const elapsed = now - startAt;
      const ratio = elapsed / o.duration;
      const current = Math.ceil(value * ratio);
      counter.innerHTML = o.render(Math.ceil(current));
      requestAnimationFrame(animate);
    }
  };
  animate();
};

export const loadOpeningLpv = (el: HTMLElement, color: Color, opening: Opening): void => {
  const lpv = Lpv(el, {
    pgn: opening.pgn,
    initialPly: 0,
    keyboardToMove: false,
    showMoves: 'bottom',
    showClocks: false,
    showPlayers: false,
    showControls: false,
    chessground: {
      coordinates: false,
    },
    orientation: color,
  });
  set(lpv.div!, 'lpv', lpv);
};
