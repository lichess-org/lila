import { h, VNodeData, thunk } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import renderTabs from './tabs';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import LobbyController from '../ctrl';
import { onInsert } from 'common/snabbdom';
import renderSetupModal from './setup/modal';
import { numberFormat } from 'common/number';

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {};
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
      case 'pools':
        body = renderPools.render(ctrl);
        data = { hook: renderPools.hooks(ctrl) };
        break;
      case 'real_time':
        body = renderRealTime(ctrl);
        break;
      case 'seeks':
        body = renderSeeks(ctrl);
        break;
      case 'now_playing':
        body = renderPlaying(ctrl);
        break;
    }
  const { trans } = ctrl;
  const { members, rounds } = ctrl.data.counters;
  return h('div.lobby__app.lobby__app-' + ctrl.tab, [
    h('div.tabs-horiz', { attrs: { role: 'tablist' } }, renderTabs(ctrl)),
    h('div.lobby__app__content.l' + (ctrl.redirecting ? 'redir' : ctrl.tab), data, body),
    renderSetupModal(ctrl),
    // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
    thunk(
      'div.lobby__counters',
      () =>
        h('div.lobby__counters', [
          lichess.blindMode ? h('h2', 'Counters') : null,
          h(
            'a',
            { attrs: lichess.blindMode ? {} : { href: '/player' } },
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
            lichess.blindMode ? {} : { attrs: { href: '/games' } },
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
    ),
  ]);
}
