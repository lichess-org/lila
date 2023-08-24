import { h } from 'snabbdom';
import { MaybeVNodes } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { localBots, type BotInfo } from 'libot';

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

function botPicker(ctrl: SetupCtrl) {
  if (lichess.blindMode) return null;
  return h(
    'div#bot-select',
    {},
    Object.values(localBots).map(bot => botView(ctrl, bot)),
  );
}

function botView(ctrl: SetupCtrl, bot: BotInfo) {
  ctrl;
  return h('div.libot', [
    h('img', { attrs: { src: bot.image } }),
    h('h3', bot.name),
    h('p', bot.description),
  ]);
}
