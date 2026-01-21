import type TournamentController from '../ctrl';
import { bind, type MaybeVNode, snabDialog } from 'lib/view';
import { fullName, userFlair } from 'lib/view/userLink';
import { h, type VNode } from 'snabbdom';
import type { TeamBattle, RankedTeam, LightTeam } from '../interfaces';

export function joinWithTeamSelector(ctrl: TournamentController) {
  const tb = ctrl.data.teamBattle!;
  const onClose = () => {
    ctrl.joinWithTeamSelector = false;
    ctrl.redraw();
  };
  return snabDialog({
    class: 'team-battle__choice',
    modal: true,
    onInsert(dlg) {
      $('.team-picker__team', dlg.view).on('click', e => {
        ctrl.join(e.target.dataset['id']);
        dlg.close();
      });
      dlg.show();
    },
    onClose,
    vnodes: [
      h('div.team-picker', [
        h('h2', i18n.arena.pickYourTeam),
        h('br'),
        ...(tb.joinWith.length
          ? [
              h('p', i18n.arena.whichTeamWillYouRepresentInThisBattle),
              ...tb.joinWith.map(id =>
                h(
                  'button.button.team-picker__team',
                  { attrs: { 'data-id': id } },
                  renderTeamArray(tb.teams[id]),
                ),
              ),
            ]
          : [
              h('p', i18n.arena.youMustJoinOneOfTheseTeamsToParticipate),
              h(
                'ul',
                shuffleArray(Object.keys(tb.teams)).map((id: string) =>
                  h('li', h('a', { attrs: { href: '/team/' + id } }, renderTeamArray(tb.teams[id]))),
                ),
              ),
            ]),
      ]),
    ],
  });
}

const renderTeamArray = (team: LightTeam) => [team[0], userFlair({ flair: team[1] })];

export function teamStanding(ctrl: TournamentController, klass?: string): VNode | null {
  const battle = ctrl.data.teamBattle,
    standing = ctrl.data.teamStanding,
    bigBattle = battle && Object.keys(battle.teams).length > 10;
  return battle && standing
    ? h('table.slist.tour__team-standing' + (klass ? '.' + klass : ''), [
        h('tbody', [
          ...standing.map(rt => teamTr(ctrl, battle, rt)),
          ...(bigBattle ? [extraTeams(ctrl), myTeam(ctrl, battle)] : []),
        ]),
      ])
    : null;
}

function extraTeams(ctrl: TournamentController): VNode {
  return h(
    'tr',
    h(
      'td.more-teams',
      { attrs: { colspan: 4 } },
      h(
        'a',
        { attrs: { href: `/tournament/${ctrl.data.id}/teams` } },
        i18n.arena.viewAllXTeams(Object.keys(ctrl.data.teamBattle!.teams).length),
      ),
    ),
  );
}

function myTeam(ctrl: TournamentController, battle: TeamBattle): MaybeVNode {
  const team = ctrl.data.myTeam;
  return team && team.rank > 10 ? teamTr(ctrl, battle, team) : undefined;
}

export function teamName(battle: TeamBattle, teamId: string): VNode {
  return h(
    battle.hasMoreThanTenTeams ? 'team' : 'team.ttc-' + Object.keys(battle.teams).indexOf(teamId),
    renderTeamArray(battle.teams[teamId]),
  );
}

function teamTr(ctrl: TournamentController, battle: TeamBattle, team: RankedTeam) {
  const players = [] as (string | VNode)[];
  team.players.forEach((p, i) => {
    if (i > 0) players.push('+');
    players.push(
      h(
        'score.ulpt.user-link',
        {
          key: p.user.name,
          class: { top: i === 0 },
          attrs: { 'data-href': '/@/' + p.user.name },
          hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
        },
        [...(i === 0 ? [h('username', fullName(p.user)), ' '] : []), '' + p.score],
      ),
    );
  });
  return h(
    'tr',
    {
      key: team.id,
      class: { active: ctrl.teamInfo.requested === team.id },
      hook: bind('click', _ => ctrl.showTeamInfo(team.id), ctrl.redraw),
    },
    [
      h('td.rank', '' + team.rank),
      h('td.team', [teamName(battle, team.id)]),
      h(
        'td.players',
        {
          hook: bind('click', e => {
            const href = (e.target as HTMLElement).getAttribute('data-href');
            if (href) {
              ctrl.jumpToPageOf(href.slice(3));
              ctrl.redraw();
            }
          }),
        },
        players,
      ),
      h('td.total', [h('strong', '' + team.score)]),
    ],
  );
}

/* Randomize array element order in-place. Using Durstenfeld shuffle algorithm. */
function shuffleArray<A>(array: A[]) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}
