import { h, VNode } from 'snabbdom';
//import * as licon from 'common/licon';
//import { bind } from 'common/snabbdom';
import { bots } from './bots';
import { Ctrl } from './ctrl';

export default function (ctrl: Ctrl): VNode {
  return h('section#bot-view', {}, [
    h('div#bot-tabs', { attrs: { role: 'tablist' } }),
    h(
      'div#bot-content',
      h(
        'div#bot-list',
        bots.map(bot => botView(ctrl, bot))
      )
    ),
  ]);
}

function botView(ctrl: Ctrl, bot: any): VNode {
  return h('div.fancy-bot', [
    h('h1', bot.name),
    h('p', bot.description),
    h('img', { attrs: { src: bot.image } }),
  ]);
}
