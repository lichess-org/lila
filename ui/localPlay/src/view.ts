import { h, VNode } from 'snabbdom';
//import * as licon from 'common/licon';
//import { bind } from 'common/snabbdom';
import { localBots } from 'libot';
import { Ctrl } from './ctrl';

export default function (ctrl: Ctrl): VNode {
  return h('section#bot-view', {}, [
    h('div#bot-tabs', { attrs: { role: 'tablist' } }),
    h(
      'div#bot-content',
      h(
        'div#bot-list',
        Object.values(localBots).map(bot => botView(ctrl, bot)),
      ),
    ),
  ]);
}

function botView(ctrl: Ctrl, bot: any): VNode {
  return h('div.fancy-bot', [
    h('h1', bot.name),
    h('p', bot.description),
    h('img', { attrs: { src: lichess.assetUrl(bot.image, { noVersion: true }) } }),
  ]);
}
