import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ChatPlugin } from 'chat'
import { justIcon } from './util'

export interface TourStandingData {
  standing: RankedPlayer[]
}

interface RankedPlayer {
  name: string;
  rank: number;
  score: number;
  title?: string;
  fire: boolean;
  withdraw: boolean;
}

export function tourStandingCtrl(data: TourStandingData, name: string): ChatPlugin {
  return {
    tab: {
      key: 'tourStanding',
      name: name
    },
    view(): VNode {
      return h('table.slist',
        h('tbody', data.standing.map(p => {
          return h('tr.' + p.name, [
            h('td.name', [
              p.withdraw ? h('span', justIcon('Z')) : h('span.rank', '' + p.rank),
            h('a.user_link.ulpt', {
              attrs: {
                href: `/@/${p.name}`
              }
            }, (p.title ? p.title + ' ' : '') + p.name)
            ]),
            h('td.total', p.fire ? {
              class: { 'is-gold': true },
              attrs: { 'data-icon': 'Q' }
            } : {}, '' + p.score)
          ])
        }))
      );
    }
  };
}
