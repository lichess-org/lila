import { bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { teamName } from './battle';
import { numberRow, player as renderPlayer } from './util';

export default function (ctrl: TournamentController): VNode | undefined {
  const battle = ctrl.data.teamBattle,
    data = ctrl.teamInfo.loaded,
    noarg = ctrl.trans.noarg;
  if (!battle) return undefined;
  const teamTag = ctrl.teamInfo.requested ? teamName(battle, ctrl.teamInfo.requested) : null;
  const tag = 'div.tour__team-info.tour__actor-info';
  if (!data || data.id !== ctrl.teamInfo.requested) return h(tag, [h('div.stats', [h('h2', [teamTag]), spinner()])]);
  const nbLeaders = ctrl.data.teamStanding?.find(s => s.id == data.id)?.players.length || 0;

  const setup = (vnode: VNode) => {
    window.lishogi.powertip.manualUserIn(vnode.elm as HTMLElement);
  };
  return h(
    tag,
    {
      hook: {
        insert: setup,
        postpatch(_, vnode) {
          setup(vnode);
        },
      },
    },
    [
      h('a.close', {
        attrs: dataIcon('L'),
        hook: bind('click', () => ctrl.showTeamInfo(data.id), ctrl.redraw),
      }),
      h('div.stats', [
        h('h2', [teamTag]),
        h('table', [
          numberRow('Players', data.nbPlayers),
          ...(data.rating
            ? [
                numberRow(noarg('averageElo'), data.rating, 'raw'),
                ...(data.perf
                  ? [
                      numberRow(noarg('averagePerformance'), data.perf, 'raw'),
                      numberRow(noarg('averageScore'), data.score, 'raw'),
                    ]
                  : []),
              ]
            : []),
          h(
            'tr',
            h(
              'th',
              h(
                'a',
                {
                  attrs: { href: '/team/' + data.id },
                },
                noarg('teamPage')
              )
            )
          ),
        ]),
      ]),
      h('div', [
        h(
          'table.players.sublist',
          data.topPlayers.map((p, i) =>
            h(
              'tr',
              {
                key: p.name,
                hook: bind('click', () => ctrl.jumpToPageOf(p.name)),
              },
              [
                h('th', '' + (i + 1)),
                h('td', renderPlayer(p, false, true, false, i < nbLeaders)),
                h('td.total', [
                  p.fire && !ctrl.data.isFinished
                    ? h('strong.is-gold', { attrs: dataIcon('Q') }, '' + p.score)
                    : h('strong', '' + p.score),
                ]),
              ]
            )
          )
        ),
      ]),
    ]
  );
}
