import { h } from 'snabbdom';
import type { BasePlayer } from '../interfaces';
import { numberFormat } from 'lib/i18n';
import { fullName, userLine, userRating } from 'lib/view/userLink';

export function player(p: BasePlayer, asLink: boolean, withRating: boolean) {
  return h(
    'a.ulpt.user-link.online' + (((p.user.title || '') + p.user.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink ? { href: '/@/' + p.user.name } : { 'data-href': '/@/' + p.user.name },
      hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
    },
    [
      p.user.patronColor && userLine({ patronColor: p.user.patronColor }),
      h('span.name', fullName(p.user)),
      withRating ? h('span.rating', userRating({ ...p, brackets: false })) : null,
    ],
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
          : numberFormat(value),
    ),
  ]);
}
