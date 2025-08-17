import { hl, VNode } from 'lib/snabbdom';

export function renderGameView(side?: VNode): VNode {
  return hl('main.round', [
    side ? hl('aside.round__side', side) : undefined,
    hl('div.round__app', [hl('div.round__app__board.main-board'), hl('div.col1-rmoves-preload')]),
    hl('div.round__underboard', [hl('div.round__now-playing')]),
    hl('div.round__underchat'),
  ]);
}
