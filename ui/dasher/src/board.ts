import { h, VNode } from 'snabbdom';
import { Redraw, Close, bind, header } from './util';
import debounce from 'common/debounce';
import * as xhr from 'common/xhr';

export interface BoardCtrl {
  data: BoardData;
  trans: Trans;
  setIs3d(v: boolean): void;
  readZoom(): number;
  setZoom(v: number): void;
  close(): void;
}

export interface BoardData {
  is3d: boolean;
}

export type PublishZoom = (v: number) => void;

export function ctrl(data: BoardData, trans: Trans, redraw: Redraw, close: Close): BoardCtrl {
  const readZoom = () => parseInt(getComputedStyle(document.body).getPropertyValue('--zoom'));

  const saveZoom = debounce(
    () =>
      xhr
        .text('/pref/zoom?v=' + readZoom(), { method: 'post' })
        .catch(() => lichess.announce({ msg: 'Failed to save zoom' })),
    1000
  );

  return {
    data,
    trans,
    setIs3d(v: boolean) {
      data.is3d = v;
      xhr
        .text('/pref/is3d', {
          body: xhr.form({ is3d: v }),
          method: 'post',
        })
        .then(lichess.reload, _ => lichess.announce({ msg: 'Failed to save geometry  preference' }));
      redraw();
    },
    readZoom,
    setZoom(v: number) {
      document.body.setAttribute('style', '--zoom:' + v);
      window.dispatchEvent(new Event('resize'));
      redraw();
      saveZoom();
    },
    close,
  };
}

export function view(ctrl: BoardCtrl): VNode {
  const domZoom = ctrl.readZoom();

  return h('div.sub.board', [
    header(ctrl.trans.noarg('boardGeometry'), ctrl.close),
    h('div.selector.large', [
      h(
        'button.text',
        {
          class: { active: !ctrl.data.is3d },
          attrs: { 'data-icon': '', type: 'button' },
          hook: bind('click', () => ctrl.setIs3d(false)),
        },
        '2D'
      ),
      h(
        'button.text',
        {
          class: { active: ctrl.data.is3d },
          attrs: { 'data-icon': '', type: 'button' },
          hook: bind('click', () => ctrl.setIs3d(true)),
        },
        '3D'
      ),
    ]),
    h(
      'div.zoom',
      isNaN(domZoom)
        ? [h('p', 'No board to zoom here!')]
        : [
            h('p', [ctrl.trans.noarg('boardSize'), ': ', domZoom, '%']),
            h('input.range', {
              attrs: {
                type: 'range',
                min: 0,
                max: 100,
                step: 1,
                value: ctrl.readZoom(),
              },
              hook: {
                insert(vnode) {
                  const input = vnode.elm as HTMLInputElement;
                  $(input).on('input', () => ctrl.setZoom(parseInt(input.value)));
                },
              },
            }),
          ]
    ),
  ]);
}
