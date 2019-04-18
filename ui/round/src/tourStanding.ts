import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { onInsert } from './util'
import { ChatPlugin } from 'chat'

export interface TourStandingCtrl extends ChatPlugin {
  set(data: TourPlayer[]): void;
}

export interface TourPlayer {
  n: string; // name
  s: number; // score
  t?: string; // title
  f: boolean; // fire
  w: boolean; // withdraw
}

export function tourStandingCtrl(data: TourPlayer[], name: string): TourStandingCtrl {
  return {
    set(d: TourPlayer[]) { data = d },
    tab: {
      key: 'tourStanding',
      name: name
    },
    view(): VNode {
      return h('table.slist', {
        hook: onInsert(_ => {
          window.lichess.loadCssPath('round.tour-standing');
        })
      }, [
        h('tbody', data.map((p: TourPlayer, i: number) => {
          return h('tr.' + p.n, [
            h('td.name', [
              h('span.rank', '' + (i + 1)),
              h('a.user-link.ulpt', {
                attrs: { href: `/@/${p.n}` }
              }, (p.t ? p.t + ' ' : '') + p.n)
            ]),
            h('td.total', p.f ? {
              class: { 'is-gold': true },
              attrs: { 'data-icon': 'Q' }
            } : {}, '' + p.s)
          ])
        }))
      ]);
    }
  };
}
