import * as cg from 'chessground/types';
import { VNodeData } from 'snabbdom';
import { Dests, EncodedDests, RoundData, Step } from './interfaces';

export { bind, onInsert } from 'common/snabbdom';

export const justIcon = (icon: string): VNodeData => ({
  attrs: { 'data-icon': icon },
});

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

export const firstPly = (d: RoundData): number => d.steps[0].ply;

export const lastPly = (d: RoundData): number => lastStep(d).ply;

export const lastStep = (d: RoundData): Step => d.steps[d.steps.length - 1];

export const plyStep = (d: RoundData, ply: number): Step => d.steps[ply - firstPly(d)];

export const massage = (d: RoundData): void => {
  if (d.clock) {
    d.clock.showTenths = d.pref.clockTenths;
    d.clock.showBar = d.pref.clockBar;
  }

  if (d.correspondence) d.correspondence.showBar = d.pref.clockBar;

  if (['horde', 'crazyhouse'].includes(d.game.variant.key)) d.pref.showCaptured = false;

  if (d.expiration) d.expiration.movedAt = Date.now() - d.expiration.idleMillis;
};
