import { h, thunk } from 'snabbdom';
import LobbyController from '../ctrl';
import { onInsert } from 'common/snabbdom';
import { numberFormat } from 'common/number';

export function renderCounters(ctrl: LobbyController) {
  // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
  const { data, trans } = ctrl;
  const { members, rounds } = data.counters;
  return thunk(
    'div.lobby__counters',
    () =>
      h('div.lobby__counters', [
        site.blindMode ? h('h2', 'Counters') : null,
        h(
          'a',
          { attrs: site.blindMode ? {} : { href: '/player' } },
          trans.vdomPlural(
            'nbPlayers',
            members,
            h(
              'strong',
              {
                attrs: { 'data-count': members },
                hook: onInsert<HTMLAnchorElement>(elm => {
                  ctrl.spreadPlayersNumber = ctrl.initNumberSpreader(elm, 10, members);
                }),
              },
              numberFormat(members),
            ),
          ),
        ),
        h(
          'a',
          site.blindMode ? {} : { attrs: { href: '/games' } },
          trans.vdomPlural(
            'nbGamesInPlay',
            rounds,
            h(
              'strong',
              {
                attrs: { 'data-count': rounds },
                hook: onInsert<HTMLAnchorElement>(elm => {
                  ctrl.spreadGamesNumber = ctrl.initNumberSpreader(elm, 8, rounds);
                }),
              },
              numberFormat(rounds),
            ),
          ),
        ),
      ]),
    [],
  );
}
