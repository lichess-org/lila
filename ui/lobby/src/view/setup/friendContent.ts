import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import userLink from 'common/userLink';
import LobbyController from '../../ctrl';
import { variantPicker } from './components/variantPicker';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function friendContent(ctrl: LobbyController): MaybeVNodes {
  const { trans } = ctrl;
  return [
    h('h2', trans('playWithAFriend')),
    h('div.setup-content', [
      ctrl.setupCtrl.friendUser ? userLink(ctrl.setupCtrl.friendUser) : null,
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      gameModeButtons(ctrl),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
