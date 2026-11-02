import { h } from 'snabbdom';

import { fullName, profileUrl, userLine, userRating } from 'lib/view/userLink';

import type { BasePlayer } from '../interfaces';

export function player(p: BasePlayer, asLink: boolean, withRating: boolean) {
  const profileHref = profileUrl(p.user.name);
  return h(
    'a.ulpt.user-link.online' + (((p.user.title || '') + p.user.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink ? { href: profileHref } : { 'data-href': profileHref },
      hook: { destroy: vnode => $.powerTip.destroy(vnode.elm) },
    },
    [
      p.user.patronColor && userLine({ patronColor: p.user.patronColor }),
      h('span.name', fullName(p.user)),
      withRating ? h('span.rating', userRating({ ...p, brackets: false })) : null,
    ],
  );
}
