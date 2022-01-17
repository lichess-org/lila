import { h, VNode } from 'snabbdom';
import spinner from 'common/spinner';
import { bind, dataIcon } from 'common/snabbdom';
import TournamentController from '../ctrl';
import { numberRow, player as renderPlayer } from './util';
import { teamName } from './battle';

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
    lichess.powertip.manualUserIn(vnode.elm as HTMLElement);
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
        attrs: dataIcon(''),
        hook: bind('click', () => ctrl.showTeamInfo(data.id), ctrl.redraw),
      }),
      h('div.stats', [
        h('h2', [teamTag]),
        h('table', [
          numberRow('Players', data.nbPlayers),
          ...(data.rating
            ? [
                ctrl.opts.showRatings ? numberRow(noarg('averageElo'), data.rating, 'raw') : null,
                ...(data.perf
                  ? [
                      ctrl.opts.showRatings ? numberRow('Average performance', data.perf, 'raw') : null,
                      numberRow('Average score', data.score, 'raw'),
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
                'Team page'
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
                h('td', renderPlayer(p, false, ctrl.opts.showRatings, false, i < nbLeaders)),
                h('td.total', [
                  p.fire && !ctrl.data.isFinished
                    ? h('strong.is-gold', { attrs: dataIcon('') }, '' + p.score)
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
