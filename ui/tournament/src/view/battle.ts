import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';

import { bind, onInsert, playerName } from './util';
import { TeamBattle, RankedTeam } from '../interfaces';
import TournamentController from '../ctrl';

export function joinWithTeamSelector(ctrl: TournamentController) {
  const onClose = () => {
    ctrl.joinWithTeamSelector = false;
    ctrl.redraw();
  };
  const tb = ctrl.data.teamBattle!;
  return h('div#modal-overlay', {
    hook: bind('click', onClose)
  }, [
    h('div#modal-wrap.team-battle__choice', {
      hook: onInsert(el => {
        el.addEventListener('click', e => e.stopPropagation());
      })
    }, [
      h('span.close', {
        attrs: { 'data-icon': 'L' },
        hook: bind('click', onClose)
      }),
      h('div.team-picker', [
        h('h2', "Pick your team"),
        h('br'),
        ...(tb.joinWith.length ? [
          h('p', "Which team will you represent in this battle?"),
          ...tb.joinWith.map(id => h('a.button', {
            hook: bind('click', () => ctrl.join(undefined, id), ctrl.redraw)
          }, tb.teams[id]))
        ] : [
            h('p', "You must join one of these teams to participate!"),
            h('ul', shuffleArray(Object.keys(tb.teams)).map((t: string) =>
              h('li', h('a', {
                attrs: { href: '/team/' + t }
              }, tb.teams[t]))
            ))
          ])
      ])
    ])
  ]);
}

export function teamStanding(ctrl: TournamentController, klass?: string): VNode | null {
  const battle = ctrl.data.teamBattle,
    standing = ctrl.data.teamStanding;
  return battle && standing ? h('table.slist.tour__team-standing' + (klass ? '.' + klass : ''), [
    h('tbody', standing.map(rt => teamTr(ctrl, battle, rt)))
  ]) : null;
}

export function teamName(battle: TeamBattle, teamId: string): VNode {
  return h('team.ttc-' + Object.keys(battle.teams).indexOf(teamId), battle.teams[teamId]);
}

function teamTr(ctrl: TournamentController, battle: TeamBattle, team: RankedTeam) {
  const players = [] as (string | VNode)[];
  team.players.forEach((p, i) => {
    if (i > 0) players.push('+');
    players.push(h('score.ulpt.user-link', {
      key: p.user.name,
      class: { top: i === 0 },
      attrs: {
        'data-href': '/@/' + p.user.name,
        'data-name': p.user.name
      },
      hook: {
        destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
        ...bind('click', _ => ctrl.jumpToPageOf(p.user.name), ctrl.redraw)
      }
    }, [
      ...(i === 0 ? [h('username', playerName(p.user)), ' '] : []),
      '' + p.score
    ]));
  });
  return h('tr', {
    key: team.id,
    class: {
      active: ctrl.teamInfo.requested == team.id
    },
    hook: bind('click', _ => ctrl.showTeamInfo(team.id), ctrl.redraw)
  }, [
    h('td.rank', '' + team.rank),
    h('td.team', [
      teamName(battle, team.id)
    ]),
    h('td.players', players),
    h('td.total', [
      h('strong', '' + team.score)
    ])
  ]);
}

/* Randomize array element order in-place. Using Durstenfeld shuffle algorithm. */
function shuffleArray<A>(array: A[]) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}
