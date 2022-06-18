import { squareFile, squareRank } from 'shogiops';
import { isDrop, Move, Square } from 'shogiops/types';
import { Hooks } from 'snabbdom/hooks';

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'sente' : 'gote';
}

export function bindMobileMousedown(el: HTMLElement, f: (e: Event) => any, redraw?: () => void): void {
  for (const mousedownEvent of ['touchstart', 'mousedown']) {
    el.addEventListener(
      mousedownEvent,
      e => {
        f(e);
        e.preventDefault();
        if (redraw) redraw();
      },
      { passive: false }
    );
  }
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el =>
    el.addEventListener(eventName, e => {
      const res = f(e);
      if (redraw) redraw();
      return res;
    })
  );
}

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function dataIcon(icon: string) {
  return {
    'data-icon': icon,
  };
}

export function scalashogiCharPair(move: Move): string {
  const charOffset = 35;
  function squareToCharCode(sq: Square): number {
    return charOffset + squareRank(sq) * 9 + squareFile(sq);
  }
  if (isDrop(move))
    return String.fromCharCode(
      squareToCharCode(move.to),
      charOffset + 81 + ['rook', 'bishop', 'gold', 'silver', 'knight', 'lance', 'pawn'].indexOf(move.role)
    );
  else {
    const from = squareToCharCode(move.from),
      to = squareToCharCode(move.to);
    if (move.promotion) return String.fromCharCode(to, from);
    else return String.fromCharCode(from, to);
  }
}
