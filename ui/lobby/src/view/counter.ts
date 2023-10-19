import { h, thunk } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import LobbyController from '../ctrl';
import { numberFormat } from 'common/number';

export function counterView(ctrl: LobbyController) {
  const { data, trans } = ctrl;
  const { members, rounds } = data.counters;
  // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
  return thunk(
    'div.lobby__counters',
    () =>
      h('div.lobby__counters', [
        lichess.blindMode ? h('h2', 'Counters') : null,
        h(
          'a',
          { attrs: lichess.blindMode ? {} : { href: '/player' } },
          trans.vdomPlural(
            'nbPlayersOnline',
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
          lichess.blindMode ? {} : { attrs: { href: '/games' } },
          trans.vdomPlural(
            'nbTotalGames',
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
