import type { Tablesort } from 'tablesort';

import { memoize, throttle } from 'lib';
import { Group, StudyBoard } from 'lib/licon';
import { dataIcon, hl, onInsert, requiresI18n, spinnerVdom, type VNode } from 'lib/view';
import { json as xhrJson } from 'lib/xhr';

import { playerFedFlag } from '@/view/util';

import * as fideFeds from '../fideFeds';
import type { Federation, StudyPlayerFromServer } from '../interfaces';
import { convertPlayerFromServer } from '../studyChapters';
import type {
  RelayTeamName,
  RelayTeamStandings,
  RelayTeamStandingsEntry,
  RelayTeamStandingsFromServer,
  TourId,
} from './interfaces';
import RelayPlayers, { renderPlayers, tableAugment, type RelayPlayer } from './relayPlayers';

export default class RelayTeamLeaderboard {
  standings: RelayTeamStandings | undefined;
  teamToShow: RelayTeamName | undefined;
  private table?: Tablesort;
  constructor(
    private readonly tourId: TourId,
    private readonly switchToTeamResultsTab: () => void,
    private readonly redraw: Redraw,
    private readonly players: RelayPlayers,
  ) {
    const locationTeam = location.hash.match(/^#team-results\/(.+)$/)?.[1];
    if (locationTeam) this.teamToShow = decodeURIComponent(locationTeam);
  }

  loadFromXhr = throttle(3 * 1000, async () => {
    this.standings = await xhrJson(`/broadcast/${this.tourId}/teams/standings`);
    const showFeds = this.looksLikeFederationTournament();
    this.standings = this.standings?.map(t => this.convertTeamFromServer(t, showFeds));
    this.table?.refresh();
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

  standingsView = (): VNode =>
    !this.standings
      ? spinnerVdom()
      : hl(
          'table.relay-tour__teams__standings.slist.slist-pad',
          {
            hook: onInsert<HTMLTableElement>(el => {
              this.table = tableAugment(el);
            }),
          },
          [
            hl('thead', [
              hl('tr', [
                hl('th.text', { attrs: dataIcon(Group) }, i18n.team.team),
                hl('th', i18n.broadcast.matches),
                hl('th', { attrs: { 'data-sort-default': 1 } }, i18n.broadcast.matchPoints),
                hl('th', i18n.broadcast.gamePoints),
              ]),
            ]),
            hl(
              'tbody',
              this.standings.map(entry =>
                hl('tr', [
                  hl('td', this.teamNameNode(entry)),
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

  teamView = (): VNode => {
    if (!this.standings) return spinnerVdom();
    const foundTeam = this.standings.find(t => t.name === this.teamToShow);
    if (!foundTeam) {
      this.teamToShow = undefined;
      return this.standingsView();
    }
    return hl('div.relay-tour__team-summary', [
      hl('div.relay-tour__team-summary', [
        hl(
          'h2.relay-tour__team-summary__header.text',
          { attrs: !this.looksLikeFederationTournament() ? dataIcon(Group) : {} },
          this.teamNameNode(foundTeam),
        ),
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
            foundTeam.matches.map((match, i) => {
              const oppTeam = this.standings?.find(t => t.name === match.opponent);
              return hl('tr', [
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
                    oppTeam ? this.teamNameNode(oppTeam) : match.opponent,
                  ),
                ),
                hl(
                  'td.score',
                  hl(match.points === '1' ? 'good' : match.points === '0' ? 'bad' : 'draw', match.mp ?? '*'),
                ),
                hl('td.score', match.gp ?? '*'),
              ]);
            }),
          ),
        ]),
      ]),
    ]);
  };

  view = (): VNode =>
    requiresI18n('team', this.redraw, () =>
      hl(
        'div.relay-tour__team__results',
        { hook: onInsert(this.loadFromXhr) },
        this.teamToShow ? this.teamView() : this.standingsView(),
      ),
    );

  private readonly teamNameNode = (team: RelayTeamStandingsEntry): VNode =>
    hl(
      'a.team-name',
      {
        on: {
          click: this.toggleTeam(team.name),
        },
      },
      [
        playerFedFlag(team.fed),
        // Don't translate names like "Hungary B".
        (team.name.toLowerCase() === team.fed?.name.toLowerCase() && team.fed.i18nName) || team.name,
      ],
    );

  private readonly convertTeamFromServer = (
    team: RelayTeamStandingsFromServer,
    showFeds: boolean,
  ): RelayTeamStandingsEntry => ({
    ...team,
    fed: showFeds ? this.teamNameToFed(team.name) : undefined,
    players: team.players.map((player: RelayPlayer & StudyPlayerFromServer) =>
      convertPlayerFromServer(player),
    ),
  });

  private readonly toggleTeam = (team: RelayTeamName) => (ev: PointerEvent) => {
    ev.preventDefault();
    this.setTeamToShow(team);
  };

  private readonly teamNameToFed = (teamName: RelayTeamName): Federation | undefined => {
    const teamNameLower = teamName.toLowerCase();
    const foundFed = Object.entries(fideFeds.federations).find(([_, [engName, _2]]) =>
      teamNameLower.startsWith(engName.toLowerCase()),
    );
    return (
      foundFed && {
        id: foundFed[0],
        name: foundFed[1][0],
        i18nName: foundFed[1][1] ? fideFeds.localizedName(foundFed[0]) : undefined,
      }
    );
  };

  private readonly looksLikeFederationTournament = memoize((): boolean => {
    if (!this.standings) return false;
    const teamsWithFed = this.standings.filter(team => !!this.teamNameToFed(team.name));
    return teamsWithFed.length / this.standings.length >= 0.8; // Don't expect team replacements to be exact matches
  });
}
