import Lpv from 'lichess-pgn-viewer';
import { Opening } from './interfaces';
import { set } from 'common/data';

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
    showMoves: 'bottom',
    showClocks: false,
    showPlayers: false,
    chessground: {
      coordinates: false,
    },
    orientation: color,
    menu: {
      getPgn: {
        enabled: true,
        fileName: opening.name + '.pgn',
      },
    },
  });
  set(lpv.div!, 'lpv', lpv);
};

export function formatDuration(seconds: number): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  let result: string[] = [];
  if (d > 0) {
    result.push(simplePlural(d, 'day'));
  }
  result.push(simplePlural(h, 'hour'));
  result.push(simplePlural(m, 'minute'));

  return result.slice(0, 2).join('<br>');
}

function simplePlural(n: number, unit: string): string {
  return `${n} ${unit}${n === 1 ? '' : 's'}`;
}
