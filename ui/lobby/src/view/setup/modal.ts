import { h } from 'snabbdom';
import { MaybeVNode, MaybeVNodes } from 'common/snabbdom';
import { userLink } from 'common/userLink';
import { snabDialog } from 'common/dialog';
import LobbyController from '../../ctrl';
import { variantPicker } from './variantPicker';
import { timePickerAndSliders } from './timePickerAndSliders';
import { gameModeButtons } from './gameModeButtons';
import { ratingDifferenceSliders } from './ratingDifferenceSliders';
import { createButtons } from './colorButtons';
import { ratingView } from './ratingView';
import { fenInput } from './fenInput';
import { levelButtons } from './levelButtons';

export default function setupModal(ctrl: LobbyController): MaybeVNode {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
  return snabDialog({
    class: 'game-setup',
    css: [{ hashed: 'lobby.setup' }],
    onClose: setupCtrl.closeModal,
    vnodes: [...views[setupCtrl.gameType](ctrl), ratingView(ctrl)],
  });
}

const views = {
  hook: (ctrl: LobbyController): MaybeVNodes => [
    h('h2', ctrl.trans('createAGame')),
    h('div.setup-content', [
      variantPicker(ctrl),
      timePickerAndSliders(ctrl),
      gameModeButtons(ctrl),
      ratingDifferenceSliders(ctrl),
      createButtons(ctrl),
    ]),
  ],
  friend: (ctrl: LobbyController): MaybeVNodes => [
    h('h2', ctrl.trans('playAFriend')),
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
    h('h2', ctrl.trans('playTheComputer')),
    h('div.setup-content', [
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      ...levelButtons(ctrl),
      createButtons(ctrl),
    ]),
  ],
  local: (): MaybeVNodes => [],
};
