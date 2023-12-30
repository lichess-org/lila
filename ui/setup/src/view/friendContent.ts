import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import { userLink } from 'common/userLink';
import { SetupCtrl } from '../ctrl';
import { variantPicker } from './components/variantPicker';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function friendContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', ctrl.root.trans('playWithAFriend')),
    h('div.setup-content', [
      ctrl.friendUser ? userLink({ name: ctrl.friendUser }) : null,
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      gameModeButtons(ctrl),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
