import { looseH as h, VNode } from 'common/snabbdom';

export function renderGameView(side?: VNode): VNode {
  return h('main.round', [
    side ? h('aside.round__side', side) : undefined,
    h('div.round__app', [h('div.round__app__board.main-board'), h('div.col1-rmoves-preload')]),
    h('div.round__underboard', [h('div.round__now-playing')]),
    h('div.round__underchat'),
  ]);
}
