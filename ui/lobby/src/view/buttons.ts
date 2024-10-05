import { bind, looseH as h } from 'common/snabbdom';
import LobbyController from '../ctrl';
import { GameType } from '../interfaces';
import renderSetupModal from './setup/modal';

export function renderButtons(ctrl: LobbyController) {
  const { trans, opts } = ctrl;
  const hasOngoingRealTimeGame = ctrl.hasOngoingRealTimeGame();
  const hookDisabled =
    opts.playban || opts.hasUnreadLichessMessage || ctrl.me?.isBot || hasOngoingRealTimeGame;
  return h('div.lobby__start', [
    site.blindMode && h('h2', 'Play'),
    ...[
      ['hook', 'createAGame', hookDisabled],
      ['friend', 'playAFriend', hasOngoingRealTimeGame],
      ['ai', 'playTheComputer', hasOngoingRealTimeGame],
    ].map(([gameType, transKey, disabled]: [GameType, string, boolean]) =>
      h(
        `button.button.button-metal.config_${gameType}`,
        {
          class: { active: ctrl.setupCtrl.gameType === gameType, disabled },
          attrs: { type: 'button' },
          hook: disabled
            ? {}
            : bind(
                site.blindMode ? 'click' : 'mousedown',
                () => {
                  if (gameType === 'local') site.asset.loadEsm('local.setup');
                  else ctrl.setupCtrl.openModal(gameType);
                },
                ctrl.redraw,
              ),
        },
        trans(transKey),
      ),
    ),
    renderSetupModal(ctrl),
  ]);
}
