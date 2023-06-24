import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import LobbyController from '../../ctrl';

import { variantPicker } from './components/variantPicker';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { gameModeButtons } from './components/gameModeButtons';
import { ratingDifferencePickersAndSliders } from './components/ratingDifferencePickersAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function hookContent(ctrl: LobbyController): MaybeVNodes {
  const { trans } = ctrl;
  return [
    h('h2', trans('createAGame')),
    h('div.setup-content', [
      variantPicker(ctrl),
      timePickerAndSliders(ctrl),
      gameModeButtons(ctrl),
      ratingDifferencePickersAndSliders(ctrl),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
