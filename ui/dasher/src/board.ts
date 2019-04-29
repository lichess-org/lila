import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, header } from './util'

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

  const saveZoom = window.lidraughts.debounce(() => {
    $.ajax({ method: 'post', url: '/pref/zoom?v=' + readZoom() });
  }, 1000);

  return {
    data,
    trans,
    setIs3d(v: boolean) {
      data.is3d = v;
      $.post('/pref/is3d', { is3d: v }, window.lidraughts.reload);
      redraw();
    },
    readZoom,
    setZoom(v: number) {
      document.body.setAttribute('style', '--zoom:' + (v - 100));
      window.lidraughts.dispatchEvent(window, 'resize');
      redraw();
      saveZoom();
    },
    close
  };
}

export function view(ctrl: BoardCtrl): VNode {
  return h('div.sub.board', [
    header(ctrl.trans.noarg('boardSize'), ctrl.close),
    h('div.zoom', [
      h('p', [
        ctrl.trans.noarg('boardSize'),
        ': ',
        (ctrl.readZoom() - 100),
        '%'
      ]),
      h('div.slider', {
        hook: { insert: vnode => makeSlider(ctrl, vnode.elm as HTMLElement) }
      })
    ])
  ]);
}

function makeSlider(ctrl: BoardCtrl, el: HTMLElement) {
  window.lidraughts.slider().done(() => {
    $(el).slider({
      orientation: 'horizontal',
      min: 100,
      max: 200,
      range: 'min',
      step: 1,
      value: ctrl.readZoom(),
      slide: (_: any, ui: any) => ctrl.setZoom(ui.value)
    });
}
