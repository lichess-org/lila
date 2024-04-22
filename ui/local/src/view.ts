import { h, VNode } from 'snabbdom';
//import * as licon from 'common/licon';
//import { bind } from 'common/snabbdom';
import { type Libot } from 'libot';
import { VsBotCtrl } from './vsBotCtrl';

export default function (ctrl: VsBotCtrl): VNode {
  return h('section#bot-view', {}, [
    h('div#bot-tabs', { attrs: { role: 'tablist' } }),
    h(
      'div#bot-content',
      h(
        'div#bot-list',
        ctrl.libot.sort().map(bot => botView(ctrl, bot)),
      ),
    ),
  ]);
}

function botView(ctrl: VsBotCtrl, bot: Libot): VNode {
  return h('div.fancy-bot', [
    h('img', { attrs: { src: bot.imageUrl } }),
    h('div.overview', [h('h2', bot.name), h('p', bot.description)]),
  ]);
}
