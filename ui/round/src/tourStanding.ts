import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { dataIcon, onInsert } from 'lib/snabbdom';
import type { ChatPlugin } from 'lib/chat/interfaces';
import type { Team, TourPlayer } from 'lib/game/game';

export interface TourStandingCtrl extends ChatPlugin {
  set(players: TourPlayer[]): void;
}

export const tourStandingCtrl = (
  players: TourPlayer[],
  team: Team | undefined,
  name: string,
): TourStandingCtrl => ({
  set(d: TourPlayer[]) {
    players = d;
  },
  key: 'tourStanding',
  name,
  view(): VNode {
    return h('div', { hook: onInsert(_ => site.asset.loadCssPath('round.tour-standing')) }, [
      team ? h('h3.text', { attrs: dataIcon(licon.Group) }, team.name) : null,
      h('table.slist', [
        h(
          'tbody',
          players.map((p: TourPlayer, i: number) =>
            h('tr.' + p.n, [
              h('td.name', [
                h('span.rank', '' + (i + 1)),
                h('a.user-link.ulpt', { attrs: { href: `/@/${p.n}` } }, (p.t ? p.t + ' ' : '') + p.n),
              ]),
              h('td.total', p.f ? { class: { 'is-gold': true }, attrs: dataIcon(licon.Fire) } : {}, '' + p.s),
            ]),
          ),
        ),
      ]),
    ]);
  },
});
