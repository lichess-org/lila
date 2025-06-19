import { h } from 'snabbdom';
import type { MaybeVNode, MaybeVNodes } from 'lib/snabbdom';
import { userLink } from 'lib/view/userLink';
import { snabDialog } from 'lib/view/dialog';
import type LobbyController from '../../ctrl';
import { variantPicker } from './components/variantPicker';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { ratingDifferenceSliders } from './components/ratingDifferenceSliders';
import { createButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { fenInput } from './components/fenInput';
import { levelButtons } from './components/levelButtons';

export default function setupModal(ctrl: LobbyController): MaybeVNode {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
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
    vnodes: [views[setupCtrl.gameType](ctrl), ratingView(ctrl)],
    onInsert: dlg => {
      setupCtrl.closeModal = dlg.close;
      dlg.show();
    },
  });
}

const views = {
  hook: (ctrl: LobbyController): MaybeVNodes => [
    h('h2#lobby-setup-modal-title', i18n.site.createAGame),
    h('div.setup-content', [
      variantPicker(ctrl),
      timePickerAndSliders(ctrl),
      gameModeButtons(ctrl),
      ratingDifferenceSliders(ctrl),
      createButtons(ctrl),
    ]),
  ],
  friend: (ctrl: LobbyController): MaybeVNodes => [
    h('h2#lobby-setup-modal-title', i18n.site.playWithAFriend),
    h('div.setup-content', [
      ctrl.setupCtrl.friendUser ? userLink({ name: ctrl.setupCtrl.friendUser, line: false }) : null,
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      gameModeButtons(ctrl),
      createButtons(ctrl),
    ]),
  ],
  ai: (ctrl: LobbyController): MaybeVNodes => [
    h('h2#lobby-setup-modal-title', i18n.site.playWithTheMachine),
    h('div.setup-content', [
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      ...levelButtons(ctrl),
      createButtons(ctrl),
    ]),
  ],
};
