import type { VNodeData } from 'snabbdom';
import type { EncodedDests, RoundData, Step } from './interfaces';

export const justIcon = (icon: string): VNodeData => ({
  attrs: { 'data-icon': icon },
});

export function parsePossibleMoves(dests?: EncodedDests): Dests {
  const dec = new Map();
  if (!dests) return dec;
  if (typeof dests === 'string')
    for (const ds of dests.split(' ')) {
      dec.set(ds.slice(0, 2), ds.slice(2).match(/.{2}/g) as Key[]);
    }
  else for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as Key[]);
  return dec;
}

export function findKingSquare(fen: string, color: Color): string | null {
  const rows = fen.split(' ')[0].split('/');
  const kingChar = color === 'white' ? 'K' : 'k';

  let rank = 8;
  for (const row of rows) {
    let fileIndex = 0;
    for (const ch of row) {
      if (/\d/.test(ch)) {
        fileIndex += parseInt(ch, 10);
      } else {
        const file = 'abcdefgh'[fileIndex];
        if (ch === kingChar) {
          return `${file}${rank}`;
        }
        fileIndex++;
      }
    }
    rank--;
  }
  return null;
}

export const firstPly = (d: RoundData): number => d.steps[0].ply;

export const lastPly = (d: RoundData): number => lastStep(d).ply;

export const lastStep = (d: RoundData): Step => d.steps[d.steps.length - 1];

export const plyStep = (d: RoundData, ply: number): Step => d.steps[ply - firstPly(d)];

export const upgradeServerData = (d: RoundData): void => {
  if (d.correspondence) d.correspondence.showBar = d.pref.clockBar;

  if (['horde', 'crazyhouse'].includes(d.game.variant.key)) d.pref.showCaptured = false;

  if (d.expiration) d.expiration.movedAt = Date.now() - d.expiration.idleMillis;
};
