import { hl, type VNode, type LooseVNodes } from 'lib/snabbdom';
import { snabDialog } from 'lib/view/dialog';
import type LobbyController from '../../ctrl';
import { variantPicker } from './components/variantPicker';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { ratingDifferenceSliders } from './components/ratingDifferenceSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { fenInput } from './components/fenInput';
import { levelButtons } from './components/levelButtons';
import { spinnerVdom } from 'lib/view/controls';

export default function setupModal(ctrl: LobbyController): VNode | null {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
  const buttonText = {
    hook: i18n.site.createLobbyGame,
    friend: setupCtrl.friendUser ? i18n.site.challengeX(setupCtrl.friendUser) : i18n.site.challengeAFriend,
    ai: i18n.site.playAgainstAI,
  }[setupCtrl.gameType];
  const disabled = !setupCtrl.valid();
  return snabDialog({
    attrs: { dialog: { 'aria-labelledBy': 'lobby-setup-modal-title', 'aria-modal': 'true' } },
    class: 'game-setup',
    css: [{ hashed: 'lobby.setup' }],
    onClose: () => {
      setupCtrl.closeModal = undefined;
      setupCtrl.gameType = null;
      setupCtrl.root.redraw();
    },
    modal: true,
    vnodes: [
      hl('h2', i18n.site.gameSetup),
      hl('div.setup-content', views[setupCtrl.gameType](ctrl)),
      hl(
        'div.footer',
        setupCtrl.loading
          ? spinnerVdom()
          : hl(
              `button.button.button-metal.lobby__start__button.lobby__start__button--${setupCtrl.friendUser ? 'friend-user' : setupCtrl.gameType}`,
              {
                attrs: { disabled },
                class: { disabled },
                on: { click: ctrl.setupCtrl.submit },
              },
              buttonText,
            ),
      ),
    ],
    onInsert: dlg => {
      setupCtrl.closeModal = dlg.close;
      dlg.show();
    },
  });
}

const views = {
  hook: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl),
    timePickerAndSliders(ctrl),
    gameModeButtons(ctrl),
    ratingView(ctrl),
    ratingDifferenceSliders(ctrl),
    colorButtons(ctrl),
  ],
  friend: (ctrl: LobbyController): LooseVNodes => [
    hl('div.config-group', [variantPicker(ctrl), fenInput(ctrl)]),
    timePickerAndSliders(ctrl, true),
    gameModeButtons(ctrl),
    colorButtons(ctrl),
  ],
  ai: (ctrl: LobbyController): LooseVNodes => [
    hl('div.config-group', [variantPicker(ctrl), fenInput(ctrl)]),
    timePickerAndSliders(ctrl, true),
    levelButtons(ctrl),
    colorButtons(ctrl),
  ],
};
