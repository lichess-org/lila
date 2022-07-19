import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';

export function tds(bits: MaybeVNodes): MaybeVNodes {
  return bits.map(function (bit) {
    return h('td', [bit]);
  });
}

export const perfIcons = {
  Blitz: ')',
  UltraBullet: '{',
  Bullet: 'T',
  Classical: '+',
  Rapid: '#',
  Minishogi: ',',
  Correspondence: ';',
};
