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
      $.post('/pref/is3d', { is3d: v }, window.lichess.reloadOtherTabs);
      applyDimension(v);
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
    h('div.selector', [
      h('a.text', {
        class: { active: !ctrl.data.is3d },
        attrs: { 'data-icon': 'E' },
        hook: bind('click', () => ctrl.setIs3d(false))
      }, '2D'),
      h('a.text', {
        class: { active: ctrl.data.is3d },
        attrs: { 'data-icon': 'E' },
        hook: bind('click', () => ctrl.setIs3d(true))
      }, '3D')
    ])
  ]);
}

function applyDimension(is3d: boolean) {

  $('body').children('.content').removeClass('is2d is3d').addClass(is3d ? 'is3d' : 'is2d');

  if (is3d && !$('link[href*="board-3d.css"]').length) {
    $('link[href*="board.css"]').clone().each(function(this: HTMLElement) {
      $(this).attr('href', $(this).attr('href').replace(/board\.css/, 'board-3d.css')).appendTo('head');
    });
  }

  window.lichess.pubsub.emit('set_zoom')();
}
