import { type MaybeVNode, bind } from 'common/snabbdom';
import { h } from 'snabbdom';
import type TournamentController from '../ctrl';
import type { Arrangement } from '../interfaces';
import { arrangementHasUser, playerName } from './util';

export function arrangementThumbnail(
  ctrl: TournamentController,
  a: Arrangement,
  asLink = false,
): MaybeVNode {
  const players = ctrl.data.standing.players.filter(p =>
      arrangementHasUser(a, p.name.toLowerCase()),
    ),
    gameDate = a.scheduledAt || a.startedAt,
    date = gameDate ? new Date(gameDate) : undefined;

  if (players.length === 2)
    return h(
      (asLink ? 'a' : 'div') + '.arr-thumb-wrap',
      {
        hook: !asLink ? bind('click', _ => ctrl.showArrangement(a)) : {},
        attrs: { href: '/tournament' + ctrl.data.id + '#' + a.id },
      },
      [
        a.name ? h('div.arr-name', a.name) : null,
        h('div.arr-players', [
          h('div.p-name.left', playerName(players[0])),
          h('div.swords', { attrs: { 'data-icon': 'U' } }),
          h('div.p-name.right', playerName(players[1])),
        ]),
        h(
          'div.arr-time',
          date
            ? h(
                'time.timeago',
                { attrs: { datetime: date.getTime() } },
                window.lishogi.timeago.format(date),
              )
            : null,
        ),
      ],
    );
  else null;
}
