import { h, VNode } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';
import { localBots, type BotInfo } from 'libot';

export function botPicker(ctrl: SetupCtrl) {
  if (lichess.blindMode) return null;
  return h(
    'div#bot-select',
    {},
    Object.values(localBots).map(bot => botView(ctrl, bot)),
  );
}

function botView(ctrl: SetupCtrl, bot: BotInfo): VNode {
  ctrl;
  return h('div.libot', [
    h('h1', bot.name),
    h('p', bot.description),
    h('img', { attrs: { src: bot.image } }),
  ]);
}
