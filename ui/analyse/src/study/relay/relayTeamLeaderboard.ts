import { dataIcon, hl, onInsert, requiresI18n, spinnerVdom, type VNode, type VNodeData } from 'lib/view';
import { Group, StudyBoard } from 'lib/licon';
import { json as xhrJson } from 'lib/xhr';
import type { RelayTeamName, RelayTeamStandings, TourId } from './interfaces';
import RelayPlayers, { renderPlayers, tableAugment, type RelayPlayer } from './relayPlayers';
import { defined, throttle } from 'lib';
import type { Federations, StudyPlayerFromServer } from '../interfaces';
import { convertPlayerFromServer } from '../studyChapters';

export default class RelayTeamLeaderboard {
  standings: RelayTeamStandings | undefined;
  teamToShow: RelayTeamName | undefined;
  constructor(
    private readonly tourId: TourId,
    private readonly switchToTeamResultsTab: () => void,
    private readonly federations: Federations | undefined,
    private readonly redraw: Redraw,
    private readonly players: RelayPlayers,
  ) {
    const locationTeam = location.hash.match(/^#team-results\/(.+)$/)?.[1];
    if (locationTeam) this.teamToShow = decodeURIComponent(locationTeam);
  }

  loadFromXhr = throttle(3 * 1000, async () => {
    this.standings = await xhrJson(`/broadcast/${this.tourId}/teams/standings`);
    this.standings?.forEach(teamEntry => {
      teamEntry.players = teamEntry.players.map((player: RelayPlayer & StudyPlayerFromServer) =>
        convertPlayerFromServer(player, this.federations),
      );
    });
    this.redraw();
  });

  tabHash = (): string =>
    this.teamToShow ? `#team-results/${encodeURIComponent(this.teamToShow)}` : '#team-results';

  closeTeam = () => {
    this.teamToShow = undefined;
  };

  setTabHash = () => {
    const hash = this.tabHash();
    if (location.hash !== hash) history.replaceState({}, '', hash);
  };

  setTeamToShow = (team: RelayTeamName) => {
    this.switchToTeamResultsTab();
    this.teamToShow = team;
    this.setTabHash();
    this.redraw();
  };

  standingsView = (): VNode => {
    if (!this.standings) {
      this.loadFromXhr();
      return spinnerVdom();
    }
    return hl(
      'table.relay-tour__teams__standings.slist.slist-pad',
      {
        hook: onInsert<HTMLTableElement>(el => {
          tableAugment(el);
          this.loadFromXhr();
        }),
      },
      [
        hl('thead', [
          hl('tr', [
            hl('th.text', { attrs: dataIcon(Group) }, `${i18n.team.team}`),
            hl('th', i18n.broadcast.matches),
            hl('th', { attrs: { 'data-sort-default': 1 } }, i18n.broadcast.matchPoints),
            hl('th', i18n.broadcast.gamePoints),
          ]),
        ]),
        hl(
          'tbody',
          this.standings.map(entry =>
            hl('tr', [
              hl(
                'td',
                hl(
                  'a.team-name',
                  {
                    on: {
                      click: this.toggleTeam(entry.name),
                    },
                  },
                  entry.name,
                ),
              ),
              hl('td', entry.matches.length),
              hl(
                'td',
                { attrs: { 'data-sort': entry.mp * 1000 + entry.gp, title: i18n.broadcast.matchPoints } },
                `${entry.mp}`,
              ),
              hl('td', { attrs: { title: i18n.broadcast.gamePoints } }, `${entry.gp}`),
            ]),
          ),
        ),
      ],
    );
  };

  teamView = (): VNode => {
    if (!this.standings) {
      this.loadFromXhr();
      return spinnerVdom();
    }
    const foundTeam = this.standings.find(t => t.name === this.teamToShow);
    if (!foundTeam) {
      this.teamToShow = undefined;
      return this.standingsView();
    }
    return hl('div.relay-tour__team-summary', [
      hl('div.relay-tour__team-summary', [
        hl('h2.relay-tour__team-summary__header.text', { attrs: dataIcon(Group) }, foundTeam.name),
        hl(
          'table.relay-tour__team-summary__header__stats',
          hl('tbody', [
            hl('tr', [hl('th', i18n.broadcast.matches), hl('td', `${foundTeam.matches.length}`)]),
            hl('tr', [hl('th', i18n.broadcast.matchPoints), hl('td', `${foundTeam.mp}`)]),
            hl('tr', [hl('th', i18n.broadcast.gamePoints), hl('td', `${foundTeam.gp}`)]),
            foundTeam.averageRating &&
              hl('tr', [hl('th', i18n.site.averageElo), hl('td', `${foundTeam.averageRating}`)]),
          ]),
        ),
      ]),
      hl('div.relay-tour__team-summary__roster', renderPlayers(this.players, foundTeam.players, true)),
      hl('h2.relay-tour__team-summary__matches__header', i18n.broadcast.matchHistory),
      hl('div.relay-tour__team-summary__matches', [
        hl('table.relay-tour__team-summary__table.slist.slist-pad', [
          hl(
            'thead',
            hl('tr', [
              hl('th'),
              hl('th', i18n.team.team),
              hl('th', i18n.broadcast.matchPoints),
              hl('th', i18n.broadcast.gamePoints),
            ]),
          ),
          hl(
            'tbody',
            foundTeam.matches.map((match, i) =>
              hl('tr', [
                hl(
                  'td.game-link',
                  hl(
                    'a.game-link text',
                    { attrs: { ...dataIcon(StudyBoard), href: `/broadcast/-/-/${match.roundId}#teams` } },
                    `${i + 1}`,
                  ),
                ),
                hl(
                  'td',
                  hl(
                    'a.team-name',
                    {
                      on: {
                        click: this.toggleTeam(match.opponent),
                      },
                    },
                    match.opponent,
                  ),
                ),
                defined(match.points) &&
                  defined(match.mp) &&
                  defined(match.gp) && [
                    hl(
                      'td.score',
                      hl(
                        `${match.points === '1' ? 'good' : match.points === '0' ? 'bad' : 'draw'}`,
                        `${match.mp}`,
                      ),
                    ),
                    hl('td.score', `${match.gp}`),
                  ],
              ]),
            ),
          ),
        ]),
      ]),
    ]);
  };

  view = (): VNode =>
    requiresI18n('team', this.redraw, () => (this.teamToShow ? this.teamView() : this.standingsView()));

  private toggleTeam = (team: RelayTeamName) => (ev: PointerEvent) => {
    ev.preventDefault();
    this.setTeamToShow(team);
  };
}

export const teamLinkData = (teamName: RelayTeamName): VNodeData => ({
  attrs: {
    href: `#team-results/${encodeURIComponent(teamName)}`,
  },
});
