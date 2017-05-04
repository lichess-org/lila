import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind } from './util'

export interface BoardCtrl {
  data: BoardData
  setIs3d(v: boolean): void
  close(): void
}

export interface BoardData {
  is3d: boolean
  zoom: number
}

export function ctrl(data: BoardData, redraw: Redraw, close: Close): BoardCtrl {

  return {
    data,
    setIs3d(v: boolean) {
      data.is3d = v;
      redraw();
    },
    close
  };
}

export function view(ctrl: BoardCtrl): VNode {

  return h('div.sub.board', [
    h('a.head.text', {
      attrs: { 'data-icon': 'I' },
      hook: bind('click', ctrl.close)
    }, 'Chess board'),
    h('div.selector', 'gnnh')
  ])
}

