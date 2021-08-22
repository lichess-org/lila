import { h } from 'snabbdom';
import { BasePlayer } from '../interfaces';
import { numberFormat } from 'common/number';

export const userName = (u: LightUser) => (u.title ? [h('span.utitle', u.title), ' ' + u.name] : [u.name]);

export function player(p: BasePlayer, asLink: boolean, withRating: boolean) {
  return h(
    'a.ulpt.user-link' + (((p.user.title || '') + p.user.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink ? { href: '/@/' + p.user.name } : { 'data-href': '/@/' + p.user.name },
      hook: {
        destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
      },
    },
    [
      h('span.name', userName(p.user)),
      withRating ? h('span.rating', ' ' + p.rating + (p.provisional ? '?' : '')) : null,
    ]
  );
}

export const ratio2percent = (r: number) => Math.round(100 * r) + '%';

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
        : numberFormat(value)
    ),
  ]);
}
