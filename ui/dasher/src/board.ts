import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind, header } from './util'

export interface BoardCtrl {
  data: BoardData
  trans: Trans
  setIs3d(v: boolean): void;
  readZoom(): number;
  setZoom(v: number): void;
  close(): void;
}

export interface BoardData {
  is3d: boolean
}

export type PublishZoom = (v: number) => void;

export function ctrl(data: BoardData, trans: Trans, redraw: Redraw, close: Close): BoardCtrl {

  const readZoom = () => parseInt(getComputedStyle(document.body).getPropertyValue('--zoom')) + 100;

  const saveZoom = window.lichess.debounce(() => {
    $.ajax({ method: 'post', url: '/pref/zoom?v=' + readZoom() });
  }, 1000);

  return {
    data,
    trans,
    setIs3d(v: boolean) {
      data.is3d = v;
      $.post('/pref/is3d', { is3d: v }, window.lichess.reload);
      redraw();
    },
    readZoom,
    setZoom(v: number) {
      document.body.setAttribute('style', '--zoom:' + (v - 100));
      window.lichess.dispatchEvent(window, 'resize');
      redraw();
      saveZoom();
    },
    close
  };
}

export function view(ctrl: BoardCtrl): VNode {

  const domZoom = ctrl.readZoom();

  return h('div.sub.board', [
    header(ctrl.trans.noarg('boardGeometry'), ctrl.close),
    h('div.selector.large', [
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
    h('div.zoom',
      isNaN(domZoom) ? [
        h('p', 'No board to zoom here!')
      ] : [
        h('p', [
          ctrl.trans.noarg('boardSize'),
          ': ',
          (domZoom - 100),
          '%'
        ]),
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
      value: ctrl.readZoom(),
      slide: (_: any, ui: any) => ctrl.setZoom(ui.value)
    });
  });
}
