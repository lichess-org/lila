import { h, thunk } from 'snabbdom';
import { bind, onInsert } from 'common/snabbdom';
import LobbyController from '../ctrl';
import { GameType } from '../interfaces';
import renderSetupModal from './setup/modal';

export default function table(ctrl: LobbyController) {
  const { data, trans, opts } = ctrl;
  const hasOngoingRealTimeGame = ctrl.hasOngoingRealTimeGame();
  const hookDisabled = opts.playban || opts.hasUnreadLichessMessage || ctrl.me?.isBot || hasOngoingRealTimeGame;
  const { members, rounds } = data.counters;
  return h('div.lobby__table', [
    h('div.bg-switch', { attrs: { title: 'Dark mode' } }, [h('div.bg-switch__track'), h('div.bg-switch__thumb')]),
    h(
      'div.lobby__start',
      (opts.blindMode ? [h('h2', 'Play')] : []).concat(
        [
          ['hook', 'createAGame', hookDisabled],
          ['friend', 'playWithAFriend', hasOngoingRealTimeGame],
          ['ai', 'playWithTheMachine', hasOngoingRealTimeGame],
        ].map(([gameType, transKey, disabled]: [GameType, string, boolean]) =>
          h(
            `a.button.button-metal.config_${gameType}`,
            {
              class: { active: ctrl.setupCtrl.gameType === gameType, disabled },
              hook: disabled
                ? {}
                : bind(opts.blindMode ? 'click' : 'mousedown', () => ctrl.setupCtrl.openModal(gameType), ctrl.redraw),
            },
            trans(transKey)
          )
        )
      )
    ),
    renderSetupModal(ctrl),
    // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
    thunk(
      'div.lobby__counters',
      () =>
        h('div.lobby__counters', [
          opts.blindMode ? h('h2', 'Counters') : null,
          h(
            'a',
            { attrs: opts.blindMode ? {} : { href: '/player' } },
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
                members
              )
            )
          ),
          h(
            'a',
            opts.blindMode ? {} : { attrs: { href: '/games' } },
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
                rounds
              )
            )
          ),
        ]),
      []
    ),
  ]);
}
