import * as cg from 'chessground/types'

import { h } from 'snabbdom'

type Redraw = () => void

const pieceScores = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0
};

export function uci2move(uci: string): cg.Key[] | undefined {
  if (!uci) return undefined;
  if (uci[1] === '@') return [uci.slice(2, 4) as cg.Key];
  return [uci.slice(0, 2), uci.slice(2, 4)] as cg.Key[];
};
export function bind(eventName: string, f: (e: Event) => void, redraw: Redraw | undefined = undefined) {
  return {
    insert: vnode => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        f(e);
        if (redraw) redraw();
      });
    }
  };
}
export function parsePossibleMoves(possibleMoves) {
  if (!possibleMoves) return {};
  for (var k in possibleMoves) {
    if (typeof possibleMoves[k] === 'object') break;
    possibleMoves[k] = possibleMoves[k].match(/.{2}/g);
  }
  return possibleMoves;
};
// {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
export function getMaterialDiff(pieces) {
  var counts = {
    king: 0,
    queen: 0,
    rook: 0,
    bishop: 0,
    knight: 0,
    pawn: 0
  }, p, role, c;
  for (var k in pieces) {
    p = pieces[k];
    counts[p.role] += (p.color === 'white' ? 1 : -1);
  }
  var diff = {
    white: {},
    black: {}
  };
  for (role in counts) {
    c = counts[role];
    if (c > 0) diff.white[role] = c;
    else if (c < 0) diff.black[role] = -c;
  }
  return diff;
};
export function getScore(pieces) {
  var score = 0;
  for (var k in pieces) {
    score += pieceScores[pieces[k].role] * (pieces[k].color === 'white' ? 1 : -1);
  }
  return score;
};
export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
