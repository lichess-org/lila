import { h } from 'snabbdom'
import { VNodeData } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import * as cg from 'draughtsground/types'
import { opposite } from 'draughtsground/util';
import { Redraw, EncodedDests, DecodedDests } from './interfaces';
import { decomposeUci } from 'draughts'

/*const pieceScores = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0
};*/
const pieceScores = {
  man: 1,
  king: 10,
  ghostman: 0,
  ghostking: 0
};

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon }
  };
}

export function uci2move(uci: string): cg.Key[] | undefined {
    if (!uci)
        return undefined;
    else
        return decomposeUci(uci);
}

export function bind(eventName: string, f: (e: Event) => void, redraw?: Redraw): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (redraw) redraw();
        return res;
      });
    }
  };
}

export function parsePossibleMoves(dests?: EncodedDests): DecodedDests {
  if (!dests) return {};
  const dec: DecodedDests = {};
  for (let k in dests) dec[k] = dests[k].match(/.{2}/g) as cg.Key[];
  return dec;
}

// {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
export function getMaterialDiff(pieces: cg.Pieces): cg.MaterialDiff {
  const diff: cg.MaterialDiff = {
    white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
    black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  };
  for (let k in pieces) {
    const p = pieces[k], them = diff[opposite(p.color)];
    if (them[p.role] > 0) them[p.role]--;
    else diff[p.color][p.role]++;
  }
  return diff;
}

export function getScore(pieces: cg.Pieces): number {
  let score = 0, k;
  for (k in pieces) {
    score += pieceScores[pieces[k].role] * (pieces[k].color === 'white' ? 1 : -1);
  }
  return score;
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
