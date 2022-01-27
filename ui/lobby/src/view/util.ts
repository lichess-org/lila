import { h } from 'snabbdom';
import { MaybeVNodes } from '../interfaces';

export function tds(bits: MaybeVNodes): MaybeVNodes {
  return bits.map(function (bit) {
    return h('td', [bit]);
  });
}

export const perfNames = {
  ultraBullet: 'UltraBullet',
  bullet: 'Bullet',
  blitz: 'Blitz',
  rapid: 'Rapid',
  classical: 'Classical',
  correspondence: 'Correspondence',
  racingKings: 'Racing Kings',
  threeCheck: 'Three-check',
  antichess: 'Antichess',
  horde: 'Horde',
  atomic: 'Atomic',
  crazyhouse: 'Crazyhouse',
  chess960: 'Chess960',
  kingOfTheHill: 'King of the Hill',
};
