import { h, VNode } from 'snabbdom';
import { onInsert } from './util';
import { ChatPlugin } from 'chat';
import { Team, TourPlayer } from 'game';

export interface TourStandingCtrl extends ChatPlugin {
  set(players: TourPlayer[]): void;
}

export function tourStandingCtrl(players: TourPlayer[], team: Team | undefined, name: string): TourStandingCtrl {
  return {
    set(d: TourPlayer[]) {
      players = d;
    },
    tab: {
      key: 'tourStanding',
      name: name,
    },
    view(): VNode {
      return h(
        'div',
        {
          hook: onInsert(_ => {
            lichess.loadCssPath('round.tour-standing');
          }),
        },
        [
          team
            ? h(
                'h3.text',
                {
                  attrs: { 'data-icon': '' },
                },
                team.name
              )
            : null,
          h('table.slist', [
            h(
              'tbody',
              players.map((p: TourPlayer, i: number) => {
                return h('tr.' + p.n, [
                  h('td.name', [
                    h('span.rank', '' + (i + 1)),
                    h(
                      'a.user-link.ulpt',
                      {
                        attrs: { href: `/@/${p.n}` },
                      },
                      (p.t ? p.t + ' ' : '') + p.n
                    ),
                  ]),
                  h(
                    'td.total',
                    p.f
                      ? {
                          class: { 'is-gold': true },
                          attrs: { 'data-icon': '' },
                        }
                      : {},
                    '' + p.s
                  ),
                ]);
              })
            ),
          ]),
        ]
      );
    },
  };
}
