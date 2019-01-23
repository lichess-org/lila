import { h } from 'snabbdom'
import { VNodeData } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import * as cg from 'chessground/types'
import { opposite } from 'chessground/util';
import { Redraw, EncodedDests, DecodedDests, MaterialDiff } from './interfaces';

const pieceScores = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0
};

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon }
  };
}

export function uci2move(uci: string): cg.Key[] | undefined {
  if (!uci) return undefined;
  if (uci[1] === '@') return [uci.slice(2, 4) as cg.Key];
  return [uci.slice(0, 2), uci.slice(2, 4)] as cg.Key[];
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
  if (typeof dests == 'string')
    dests.split(' ').forEach(ds => {
      dec[ds.slice(0,2)] = ds.slice(2).match(/.{2}/g) as cg.Key[];
    });
    else for (let k in dests) dec[k] = dests[k].match(/.{2}/g) as cg.Key[];
  return dec;
}

// {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
export function getMaterialDiff(pieces: cg.Pieces): MaterialDiff {
  const diff: MaterialDiff = {
    white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
    black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  };
  for (let k in pieces) {
    const p = pieces[k]!, them = diff[opposite(p.color)];
    if (them[p.role] > 0) them[p.role]--;
    else diff[p.color][p.role]++;
  }
  return diff;
}

export function getScore(pieces: cg.Pieces): number {
  let score = 0, k;
  for (k in pieces) {
    score += pieceScores[pieces[k]!.role] * (pieces[k]!.color === 'white' ? 1 : -1);
  }
  return score;
}

export function spinner() {
  return h('div.spinner', {
    'aria-label': 'loading'
  }, [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
