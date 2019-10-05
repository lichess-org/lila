import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';

import { bind, numberRow, spinner, dataIcon, player as renderPlayer } from './util';
import TournamentController from '../ctrl';

export default function(ctrl: TournamentController): VNode {
  const battle = ctrl.data.teamBattle,
    data = ctrl.teamInfo.loaded,
    noarg = ctrl.trans.noarg;
  if (!battle) return h('div');
  const teamName = ctrl.teamInfo.requested ? h('team', battle.teams[ctrl.teamInfo.requested]) : null;
  const tag = 'div.tour__team-info.tour__actor-info';
  if (!data || data.id !== ctrl.teamInfo.requested) return h(tag, [
    h('div.stats', [
      teamName,
      spinner()
    ])
  ]);
  const setup = (vnode: VNode) => {
    window.lichess.powertip.manualUserIn(vnode.elm as HTMLElement);
  }
  return h(tag, {
    hook: {
      insert: setup,
      postpatch(_, vnode) { setup(vnode) }
    }
  }, [
    h('a.close', {
      attrs: dataIcon('L'),
      hook: bind('click', () => ctrl.showTeamInfo(data.id), ctrl.redraw)
    }),
    h('div.stats', [
      h('h2', [
        teamName
      ]),
      h('table', [
        numberRow("Players", data.nbPlayers),
        numberRow(noarg('averageElo'), data.rating, 'raw'),
        numberRow("Average performance", data.perf, 'raw'),
        numberRow("Average score", data.score, 'raw'),
        h('tr', h('th', h('a', {
          attrs: { href: '/team/' + data.id }
        }, 'Team page')))
      ])
    ]),
    h('div', [
      h('table.players.sublist', {
        hook: bind('click', e => {
          const username = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-name');
          if (username) ctrl.jumpToPageOf(username);
        })
      }, data.topPlayers.map(function(p, i) {
        return h('tr', {
          key: p.name
        }, [
          h('th', '' + (i + 1)),
          h('td', renderPlayer(p, false, true, false)),
          h('td.total', [
            p.fire && !ctrl.data.isFinished ?
            h('strong.is-gold', { attrs: dataIcon('Q') }, '' + p.score) :
            h('strong', '' + p.score)
          ])
        ]);
      }))
    ])
  ]);
}
