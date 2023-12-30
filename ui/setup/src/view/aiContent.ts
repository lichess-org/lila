import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { variantPicker } from './components/variantPicker';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { levelButtons } from './components/levelButtons';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function aiContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', ctrl.root.trans('playWithTheMachine')),
    h('div.setup-content', [
      variantPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      ...levelButtons(ctrl),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
