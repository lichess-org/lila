import { looseH as h, VNode } from 'common/snabbdom';
//import * as licon from 'common/licon';
//import { bind } from 'common/snabbdom';
import type { Libot } from './types';
import type { GameCtrl } from './gameCtrl';

export default function (ctrl?: GameCtrl, side?: VNode): VNode {
  return h('main.round', [
    h(
      'aside.round__side',
      side ??
        h('section#bot-view', [
          h(
            'div#bot-content',
            h(
              'div#bot-list',
              Object.values(ctrl?.botCtrl.bots ?? {}).map(bot => botView(ctrl!, bot)),
            ),
          ),
        ]),
    ),
    h('div.round__app', [h('div.round__app__board.main-board'), h('div.col1-rmoves-preload')]),
    h('div.round__underboard', [h('div.round__now-playing')]),
    h('div.round__underchat'),
  ]);
}

function botView(ctrl: GameCtrl, bot: Libot): VNode {
  const imageUrl = ctrl.botCtrl.imageUrl(bot);
  return h('div.fancy-bot', [
    imageUrl && h('img', { attrs: { src: imageUrl } }),
    h('div.overview', [h('h2', bot.name), h('p', bot.description)]),
  ]);
}
