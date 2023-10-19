import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import LobbyController from '../ctrl';
import { GameType } from '../interfaces';
import renderSetupModal from './setup/modal';

export default function table(ctrl: LobbyController) {
  const { trans, opts } = ctrl;
  const hasOngoingRealTimeGame = ctrl.hasOngoingRealTimeGame();
  const hookDisabled =
    opts.playban || opts.hasUnreadLichessMessage || ctrl.me?.isBot || hasOngoingRealTimeGame;
  return h('div.lobby__table', [
    h(
      'div.lobby__start',
      (lichess.blindMode ? [h('h2', 'Play')] : []).concat(
        [
          ['hook', 'createAGame', hookDisabled],
          ['friend', 'playWithAFriend', hasOngoingRealTimeGame],
          ['ai', 'playWithTheMachine', hasOngoingRealTimeGame],
        ].map(([gameType, transKey, disabled]: [GameType, string, boolean]) =>
          h(
            `button.button.button-metal.config_${gameType}`,
            {
              class: { active: ctrl.setupCtrl.gameType === gameType, disabled },
              attrs: { type: 'button' },
              hook: disabled
                ? {}
                : bind(
                    lichess.blindMode ? 'click' : 'mousedown',
                    () => ctrl.setupCtrl.openModal(gameType),
                    ctrl.redraw,
                  ),
            },
            trans(transKey),
          ),
        ),
      ),
    ),
    renderSetupModal(ctrl),
  ]);
}
