import { hl, onInsert, spinnerVdom, type VNode } from 'lib/view';
import { json as xhrJson } from 'lib/xhr';
import type { RelayTeamName, RelayTeamStandings, RoundId, TourId } from './interfaces';
import { tableAugment } from './relayPlayers';

export default class RelayTeamsStandings {
  standings: RelayTeamStandings | undefined;
  teamToShow: RelayTeamName | undefined;
  constructor(
    private readonly tourId: TourId,
    readonly hideResultsSinceRoundId: () => RoundId | undefined,
    private readonly redraw: Redraw,
  ) {}

  async loadFromXhr() {
    this.standings = await xhrJson(`/broadcast/${this.tourId}/teams/standings`);
    this.redraw();
  }
}

export const standingsView = (ctrl: RelayTeamsStandings): VNode => {
  const standings = ctrl.standings;
  if (!standings) {
    ctrl.loadFromXhr();
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
          hl('th', 'Team'),
          hl('th', 'Matches'),
          hl('th', { attrs: { 'data-sort-default': 1, title: 'Match points' } }, 'MP'),
          hl('th', { attrs: { title: 'Game points' } }, 'GP'),
        ]),
      ]),
      hl(
        'tbody',
        standings.map(entry =>
          hl('tr', [
            hl('td', entry.name),
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
