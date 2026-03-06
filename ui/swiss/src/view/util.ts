import { h } from 'snabbdom';

import { fullName, userLine, userRating } from 'lib/view/userLink';

import type { BasePlayer } from '../interfaces';

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
