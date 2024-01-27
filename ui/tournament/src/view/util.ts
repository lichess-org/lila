import { h, VNode, VNodeChildren } from 'snabbdom';
import * as licon from 'common/licon';
import { numberFormat } from 'common/number';
import { dataIcon } from 'common/snabbdom';
import { fullName, userRating } from 'common/userLink';
import { SimplePlayer } from '../interfaces';

export const ratio2percent = (r: number) => Math.round(100 * r) + '%';

export const player = (
  p: SimplePlayer,
  asLink: boolean,
  withRating: boolean,
  defender = false,
  leader = false,
) =>
  h(
    'a.ulpt.user-link' + (((p.title || '') + p.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink || 'ontouchstart' in window ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
      hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
    },
    [
      h(
        'span.name' + (defender ? '.defender' : leader ? '.leader' : ''),
        defender ? { attrs: dataIcon(licon.Shield) } : leader ? { attrs: dataIcon(licon.Crown) } : {},
        fullName(p),
      ),
      withRating ? h('span.rating', userRating({ ...p, brackets: false })) : null,
    ],
  );

export function numberRow(name: string, value: number): VNode;
export function numberRow(name: string, value: [number, number], typ: 'percent'): VNode;
export function numberRow(name: string, value: VNodeChildren, typ: 'raw'): VNode;
export function numberRow(name: string, value: any, typ?: string) {
  return h('tr', [
    h('th', name),
    h(
      'td',
      typ === 'raw'
        ? value
        : typ === 'percent'
        ? value[1] > 0
          ? ratio2percent(value[0] / value[1])
          : 0
        : numberFormat(value),
    ),
  ]);
}
