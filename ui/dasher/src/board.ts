import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind, header } from './util'

export interface BoardCtrl {
  data: BoardData
  setIs3d(v: boolean): void
  setZoom(v: number): void
  close(): void
}

export interface BoardData {
  is3d: boolean
  zoom: number
}

export type PublishZoom = (v: number) => void;

export function ctrl(data: BoardData, publishZoom: PublishZoom, redraw: Redraw, close: Close): BoardCtrl {

  const saveZoom = window.lichess.fp.debounce(() => {
    $.ajax({ method: 'post', url: '/pref/zoom?v=' + data.zoom });
  }, 500);

  return {
    data,
    setIs3d(v: boolean) {
      data.is3d = v;
      $.post('/pref/is3d', { is3d: v }, window.lichess.reloadOtherTabs);
      applyDimension(v);
      publishZoom(data.zoom / 100);
      redraw();
    },
    setZoom(v: number) {
      data.zoom = v;
      publishZoom(v / 100);
      saveZoom();
    },
    close
  };
}

export function view(ctrl: BoardCtrl): VNode {

  return h('div.sub.board', [
    header('Board geometry', ctrl.close),
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
    ]),
    h('div.zoom', [
      h('h2', 'Board size'),
      h('div.slider', {
        hook: { insert: vnode => makeSlider(ctrl, vnode.elm as HTMLElement) }
      })
    ])
  ]);
}

function makeSlider(ctrl: BoardCtrl, el: HTMLElement) {
  window.lichess.slider().done(() => {
    $(el).slider({
      orientation: 'horizontal',
      min: 100,
      max: 200,
      range: 'min',
      step: 1,
      value: ctrl.data.zoom,
      slide: (_: any, ui: any) => ctrl.setZoom(ui.value)
    });
  });
}

function applyDimension(is3d: boolean) {

  $('body').children('.content').removeClass('is2d is3d').addClass(is3d ? 'is3d' : 'is2d');

  if (is3d && !$('link[href*="board-3d.css"]').length) {
    $('link[href*="board.css"]').clone().each(function(this: HTMLElement) {
      $(this).attr('href', $(this).attr('href').replace(/board\.css/, 'board-3d.css')).appendTo('head');
    });
  }
}
