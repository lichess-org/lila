import * as cg from 'chessground/types';
import { VNodeData } from 'snabbdom';
import { Dests, EncodedDests } from './interfaces';

export { bind, onInsert } from 'common/snabbdom';

export const justIcon = (icon: string): VNodeData => ({
  attrs: { 'data-icon': icon },
});

export const uci2move = (uci: string): cg.Key[] | undefined => {
  if (!uci) return undefined;
  if (uci[1] === '@') return [uci.slice(2, 4) as cg.Key];
  return [uci.slice(0, 2), uci.slice(2, 4)] as cg.Key[];
};

export function parsePossibleMoves(dests?: EncodedDests): Dests {
  const dec = new Map();
  if (!dests) return dec;
  if (typeof dests == 'string')
    for (const ds of dests.split(' ')) {
      dec.set(ds.slice(0, 2), ds.slice(2).match(/.{2}/g) as cg.Key[]);
    }
  else for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as cg.Key[]);
  return dec;
}
