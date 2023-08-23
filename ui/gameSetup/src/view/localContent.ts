import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { botPicker } from './components/botPicker';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';

export default function localContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', 'Local Play'),
    h('div.setup-content', [
      botPicker(ctrl),
      fenInput(ctrl),
      timePickerAndSliders(ctrl, true),
      colorButtons(ctrl),
    ]),
    ratingView(ctrl),
  ];
}
