import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { bind, dataIcon } from 'lib/snabbdom';
import type TournamentController from '../ctrl';
import { numberRow, player as renderPlayer } from './util';
import { teamName } from './battle';

export default function (ctrl: TournamentController): VNode | undefined {
  const battle = ctrl.data.teamBattle,
    data = ctrl.teamInfo.loaded;
  if (!battle) return undefined;
  const teamTag = ctrl.teamInfo.requested ? teamName(battle, ctrl.teamInfo.requested) : null;
  const tag = 'div.tour__team-info.tour__actor-info';
  if (!data || data.id !== ctrl.teamInfo.requested)
    return h(tag, [h('div.stats', [h('h2', [teamTag]), spinner()])]);
  const nbLeaders = ctrl.data.teamStanding?.find(s => s.id === data.id)?.players.length || 0;

  const setup = (vnode: VNode) => {
    site.powertip.manualUserIn(vnode.elm as HTMLElement);
  };
  return h(tag, { hook: { insert: setup, postpatch: (_, vnode) => setup(vnode) } }, [
    h('a.close', {
      attrs: dataIcon(licon.X),
      hook: bind('click', () => ctrl.showTeamInfo(data.id), ctrl.redraw),
    }),
    h('div.stats', [
      h('h2', h('a', { attrs: { href: `/team/${data.id}` } }, teamTag)),
      h('table', [
        numberRow(i18n.site.players, data.nbPlayers),
        ...(data.rating
          ? [
              ctrl.opts.showRatings ? numberRow(i18n.site.averageElo, data.rating, 'raw') : null,
              ...(data.perf
                ? [
                    ctrl.opts.showRatings ? numberRow(i18n.arena.averagePerformance, data.perf, 'raw') : null,
                    numberRow(i18n.arena.averageScore, data.score, 'raw'),
                  ]
                : []),
            ]
          : []),
      ]),
      data.joined
        ? 'You are part of this team'
        : h(
            'form',
            {
              attrs: {
                method: 'post',
                action: `/team/${data.id}/join?referrer=${location.pathname}#team/${data.id}`,
              },
            },
            [h('button.button.button-empty', { attrs: { type: 'submit' } }, i18n.team.joinTeam)],
          ),
    ]),
    h('div', [
      h(
        'table.players.sublist',
        data.topPlayers.map((p, i) =>
          h('tr', { key: p.name, hook: bind('click', () => ctrl.jumpToPageOf(p.name)) }, [
            h('th', '' + (i + 1)),
            h('td', renderPlayer(p, false, ctrl.opts.showRatings, false, i < nbLeaders)),
            h('td.total', [
              p.fire && !ctrl.data.isFinished
                ? h('strong.is-gold', { attrs: dataIcon(licon.Fire) }, '' + p.score)
                : h('strong', '' + p.score),
            ]),
          ]),
        ),
      ),
    ]),
  ]);
}
