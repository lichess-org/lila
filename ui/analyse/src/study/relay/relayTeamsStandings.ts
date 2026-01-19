import { dataIcon, hl, onInsert, spinnerVdom, type VNode } from 'lib/view';
import { Group, StudyBoard } from 'lib/licon';
import { json as xhrJson } from 'lib/xhr';
import type { RelayTeamName, RelayTeamStandings, RelayTeamStandingsEntry, TourId } from './interfaces';
import RelayPlayers, { renderPlayers, tableAugment, type RelayPlayer } from './relayPlayers';
import { defined } from 'lib';
import type { Federations, StudyPlayerFromServer } from '../interfaces';
import { convertPlayerFromServer } from '../studyChapters';

export default class RelayTeamsStandings {
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

  async loadFromXhr() {
    this.standings = await xhrJson(`/broadcast/${this.tourId}/teams/standings`);
    this.standings?.forEach(teamEntry => {
      teamEntry.players = teamEntry.players.map((player: RelayPlayer & StudyPlayerFromServer) =>
        convertPlayerFromServer(player, this.federations),
      );
    });
    this.redraw();
  }

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
    const standings = this.standings;
    if (!standings) {
      this.loadFromXhr();
      return spinnerVdom();
    }
    return hl(
      'table.relay-tour__teams__standings.slist.slist-pad',
      {
        hook: onInsert<HTMLTableElement>(tableAugment),
      },
      [
        hl('thead', [
          hl('tr', [
            hl('th.text', { attrs: dataIcon(Group) }, 'Team'),
            hl('th', 'Matches'),
            hl('th', { attrs: { 'data-sort-default': 1, title: 'Match points' } }, 'MP'),
            hl('th', { attrs: { title: 'Game points' } }, 'GP'),
          ]),
        ]),
        hl(
          'tbody',
          standings.map(entry =>
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
                { attrs: { 'data-sort': entry.mp * 1000 + entry.gp, title: 'Match points' } },
                `${entry.mp}`,
              ),
              hl('td', { attrs: { title: 'Game points' } }, `${entry.gp}`),
            ]),
          ),
        ),
      ],
    );
  };

  private rosterView(team: RelayTeamStandingsEntry) {
    return hl('div.relay-tour__team-summary__roster', renderPlayers(this.players, team.players));
  }

  teamView = (): VNode => {
    if (!this.standings) {
      this.loadFromXhr();
      return spinnerVdom();
    }
    const foundTeam = this.standings.find(t => t.name === this.teamToShow);
    if (!foundTeam) return this.standingsView();
    return hl('div.relay-tour__team-summary', [
      hl('div.relay-tour__team-summary', [
        hl('h2.relay-tour__team-summary__header text', { attrs: dataIcon(Group) }, foundTeam.name),
        hl('div', this.rosterView(foundTeam)),
      ]),
      hl('div.relay-tour__team-summary__matches', [
        hl('h2.relay-tour__team-summary__matches__header', 'Matches'),
        hl('table.relay-tour__team-summary__table.slist.slist-pad', [
          hl(
            'thead',
            hl('tr', [hl('th', 'Match'), hl('th', 'Opposing Team'), hl('th', `MP`), hl('th', `GP`)]),
          ),
          hl(
            'tbody',
            foundTeam.matches.map((match, i) =>
              hl('tr', [
                hl(
                  'td',
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

  view = (): VNode => (this.teamToShow ? this.teamView() : this.standingsView());

  private toggleTeam = (team: RelayTeamName) => (ev: PointerEvent) => {
    ev.preventDefault();
    this.setTeamToShow(team);
  };
}
