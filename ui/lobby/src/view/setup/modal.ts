import { hl, type VNode, type LooseVNodes, snabDialog, spinnerVdom } from 'lib/view';
import type LobbyController from '@/ctrl';
import { variantPicker } from './components/variantPicker';
import { gameModeButtons } from './components/gameModeButtons';
import { ratingDifferenceSliders } from './components/ratingDifferenceSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { fenInput } from './components/fenInput';
import { levelButtons } from './components/levelButtons';
import { timePickerAndSliders } from 'lib/setup/view/timeControl';

export default function setupModal(ctrl: LobbyController): VNode[] | null {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
  const buttonText = {
    hook: i18n.site.createLobbyGame,
    friend: setupCtrl.friendUser ? i18n.site.challengeX(setupCtrl.friendUser) : i18n.site.challengeAFriend,
    ai: i18n.site.playAgainstComputer,
  }[setupCtrl.gameType];
  const disabled = !setupCtrl.valid() || setupCtrl.loading;
  return [
    snabDialog({
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
        hl('h2#lobby-setup-modal-title', i18n.site.gameSetup),
        hl('div.setup-content', views[setupCtrl.gameType](ctrl)),
        hl('div.footer', [
          hl(
            `button.button.button-metal.lobby__start__button.lobby__start__button--${setupCtrl.friendUser ? 'friend-user' : setupCtrl.gameType}`,
            {
              attrs: { disabled },
              class: { disabled },
              on: { click: ctrl.setupCtrl.submit },
            },
            buttonText,
          ),
          setupCtrl.loading && spinnerVdom(),
        ]),
      ],
      onInsert: dlg => {
        setupCtrl.closeModal = dlg.close;
        dlg.show();
      },
    }),
  ].filter(v => v !== null) as VNode[];
}

const views = {
  hook: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, 0),
    gameModeButtons(ctrl),
    ratingView(ctrl),
    ratingDifferenceSliders(ctrl),
    colorButtons(ctrl),
  ],
  friend: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl),
    fenInput(ctrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, 0),
    gameModeButtons(ctrl),
    colorButtons(ctrl),
  ],
  ai: (ctrl: LobbyController): LooseVNodes => [
    variantPicker(ctrl),
    fenInput(ctrl),
    timePickerAndSliders(ctrl.setupCtrl.timeControl, ctrl.setupCtrl.minimumTimeIfReal()),
    levelButtons(ctrl),
    colorButtons(ctrl),
  ],
};
