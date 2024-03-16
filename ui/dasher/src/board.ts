import { h, VNode } from 'snabbdom';
import { Close, header } from './util';
import debounce from 'common/debounce';
import * as licon from 'common/licon';
import * as xhr from 'common/xhr';
import { bind, Redraw } from 'common/snabbdom';

export interface BoardData {
  is3d: boolean;
}

export class BoardCtrl {
  constructor(
    readonly data: BoardData,
    readonly trans: Trans,
    readonly redraw: Redraw,
    readonly close: Close,
  ) {}

  readZoom = () => parseInt(window.getComputedStyle(document.body).getPropertyValue('--zoom'));

  saveZoom = debounce(
    () =>
      xhr
        .text('/pref/zoom?v=' + this.readZoom(), { method: 'post' })
        .catch(() => site.announce({ msg: 'Failed to save zoom' })),
    1000,
  );

  setIs3d = (v: boolean) => {
    this.data.is3d = v;
    xhr
      .text('/pref/is3d', { body: xhr.form({ is3d: v }), method: 'post' })
      .then(site.reload, _ => site.announce({ msg: 'Failed to save geometry  preference' }));
    this.redraw();
  };

  setZoom = (v: number) => {
    document.body.style.setProperty('--zoom', v.toString());
    window.dispatchEvent(new Event('resize'));
    this.redraw();
    this.saveZoom();
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
          attrs: { 'data-icon': licon.Checkmark, type: 'button' },
          hook: bind('click', () => ctrl.setIs3d(false)),
        },
        '2D',
      ),
      h(
        'button.text',
        {
          class: { active: ctrl.data.is3d },
          attrs: { 'data-icon': licon.Checkmark, type: 'button' },
          hook: bind('click', () => ctrl.setIs3d(true)),
        },
        '3D',
      ),
    ]),
    h(
      'div.zoom',
      isNaN(domZoom)
        ? [h('p', 'No board to zoom here!')]
        : [
            h('p', [ctrl.trans.noarg('boardSize'), ': ', domZoom, '%']),
            h('input.range', {
              attrs: { type: 'range', min: 0, max: 100, step: 1, value: ctrl.readZoom() },
              hook: {
                insert(vnode) {
                  const input = vnode.elm as HTMLInputElement;
                  $(input).on('input', () => ctrl.setZoom(parseInt(input.value)));
                },
              },
            }),
          ],
    ),
  ]);
}
