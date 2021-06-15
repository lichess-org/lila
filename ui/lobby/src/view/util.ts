import { h, Hooks } from 'snabbdom';
import { MaybeVNodes } from '../interfaces';

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (redraw) redraw();
        return res;
      });
    },
  };
}

export function tds(bits: MaybeVNodes): MaybeVNodes {
  return bits.map(function (bit) {
    return h('td', [bit]);
  });
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' },
      }),
    ]),
  ]);
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
