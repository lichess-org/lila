import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';

import { variantPicker } from './components/variantPicker';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { ratingDifferenceSliders } from './components/ratingDifferenceSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function hookContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', ctrl.root.trans('createAGame')),
    h('div.setup-content', [
      variantPicker(ctrl),
      timePickerAndSliders(ctrl),
      gameModeButtons(ctrl),
      ratingDifferenceSliders(ctrl),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
