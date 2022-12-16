import { bind } from 'common/snabbdom';
import { opposite } from 'shogiground/util';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { Duel, DuelPlayer, DuelTeams, TeamBattle } from '../interfaces';
import { teamName } from './battle';
import { miniBoard, player as renderPlayer } from './util';

function featuredPlayer(player) {
  return h('div.tour__featured__player', [
    h('strong', '#' + player.rank),
    renderPlayer(player, true, true, false),
    player.berserk
      ? h('i', {
          attrs: {
            'data-icon': '`',
            title: 'Berserk',
          },
        })
      : null,
  ]);
}

function featured(f): VNode {
  return h('div.tour__featured', [featuredPlayer(f[opposite(f.color)]), miniBoard(f), featuredPlayer(f[f.color])]);
}

function duelPlayerMeta(p: DuelPlayer) {
  return [h('em.rank', '#' + p.k), p.t ? h('em.title', p.t) : null, h('em.rating', '' + p.r)];
}

function renderDuel(battle?: TeamBattle, duelTeams?: DuelTeams) {
  return (d: Duel) =>
    h(
      'a.glpt',
      {
        key: d.id,
        attrs: { href: '/' + d.id },
      },
      [
        battle && duelTeams
          ? h(
              'line.t',
              [0, 1].map(i => teamName(battle, duelTeams[d.p[i].n.toLowerCase()]))
            )
          : undefined,
        h('line.a', [h('strong', d.p[0].n), h('span', duelPlayerMeta(d.p[1]).reverse())]),
        h('line.b', [h('span', duelPlayerMeta(d.p[0])), h('strong', d.p[1].n)]),
      ]
    );
}

export default function (ctrl: TournamentController): VNode {
  return h('div.tour__table', [
    ctrl.data.featured ? featured(ctrl.data.featured) : null,
    ctrl.data.duels.length
      ? h(
          'section.tour__duels',
          {
            hook: bind('click', _ => !ctrl.disableClicks),
          },
          [h('h2', 'Top games')].concat(ctrl.data.duels.map(renderDuel(ctrl.data.teamBattle, ctrl.data.duelTeams)))
        )
      : null,
  ]);
}
