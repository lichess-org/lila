import { looseH as h, VNode, bind } from 'common/snabbdom';
//import * as licon from 'common/licon';
//import { bind } from 'common/snabbdom';
import type { BotInfo } from './types';
import type { GameCtrl } from './gameCtrl';
import { env } from './localEnv';

export function renderGameView(side?: VNode): VNode {
  return h('main.round', [
    side ? h('aside.round__side', side) : undefined /* ??
        h('section#bot-view', [
          h(
            'div#bot-content',
            h(
              'div#bot-list',
              Object.values(ctrl?.botCtrl.bots ?? {}).map(bot => botView(ctrl!, bot)),
            ),
          ),
        ]),
    ),*/,
    h('div.round__app', [h('div.round__app__board.main-board'), h('div.col1-rmoves-preload')]),
    h('div.round__underboard', [h('div.round__now-playing')]),
    h('div.round__underchat'),
  ]);
}

function botView(bot: BotInfo): VNode {
  const imageUrl = env.bot.imageUrl(bot);
  return h('div.fancy-bot', [
    imageUrl && h('img', { attrs: { src: imageUrl } }),
    h('div.overview', [h('h2', bot.name), h('p', bot.description)]),
  ]);
}

export const rangeTicks: { [type: string]: [number, string][] } = {
  initial: [
    [15, '15 seconds'],
    [30, '30 seconds'],
    [45, '45 seconds'],
    [60, '1 minute'],
    [120, '2 minutes'],
    [180, '3 minutes'],
    [300, '5 minutes'],
    [600, '10 minutes'],
    [1800, '30 minutes'],
    [3600, '60 minutes'],
    [5400, '90 minutes'],
    [Infinity, 'unlimited'],
  ],
  increment: [
    [0, 'none'],
    [1, '1 second'],
    [2, '2 seconds'],
    [3, '3 seconds'],
    [5, '5 seconds'],
    [10, '10 seconds'],
    [30, '30 seconds'],
  ],
};
