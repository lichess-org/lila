import { h, thunk } from 'snabbdom';
import { bind, onInsert } from 'lib/snabbdom';
import type LobbyController from '../ctrl';
import type { GameType } from '../interfaces';
import renderSetupModal from './setup/modal';
import { numberFormat } from 'lib/i18n';

export default function table(ctrl: LobbyController) {
  const { data, opts } = ctrl;
  const hasOngoingRealTimeGame = ctrl.hasOngoingRealTimeGame();
  const hookDisabled =
    opts.playban || opts.hasUnreadLichessMessage || ctrl.me?.isBot || hasOngoingRealTimeGame;
  const { members, rounds } = data.counters;
  return h('div.lobby__table', [
    h(
      'div.lobby__start',
      (site.blindMode ? [h('h2', i18n.site.play)] : []).concat(
        [
          ['hook', i18n.site.createLobbyGame, hookDisabled],
          ['friend', i18n.site.challengeAFriend, hasOngoingRealTimeGame],
          ['ai', i18n.site.playAgainstComputer, hasOngoingRealTimeGame],
          ...(opts.bots ? [['bots', 'play bot', false]] : []),
          ...(opts.botEditor ? [['dev', 'bot development', false]] : []),
        ].map(([gameType, text, disabled]: [GameType | 'dev' | 'bots', string, boolean]) =>
          h(
            `button.button.button-metal.lobby__start__button.lobby__start__button--${gameType}`,
            {
              class: { active: ctrl.setupCtrl.gameType === gameType, disabled },
              attrs: { type: 'button' },
              hook: disabled
                ? {}
                : bind(
                    'click',
                    () => {
                      if (gameType === 'bots') location.href = '/bots';
                      else if (gameType === 'dev') location.href = '/bots/dev';
                      else ctrl.setupCtrl.openModal(gameType);
                    },
                    ctrl.redraw,
                  ),
            },
            text,
          ),
        ),
      ),
    ),
    renderSetupModal(ctrl),
    // Use a thunk here so that snabbdom does not rerender; we will do so manually after insert
    site.blindMode
      ? undefined
      : thunk(
          'div.lobby__counters',
          () =>
            h('div.lobby__counters', [
              h(
                'a',
                { attrs: site.blindMode ? {} : { href: '/player' } },
                i18n.site.nbPlayers.asArray(
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
                i18n.site.nbGamesInPlay.asArray(
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
