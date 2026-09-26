import { h } from 'snabbdom';

import { licon } from 'lib/licon';
import { dataIcon, type MaybeVNodes } from 'lib/view';
import { userFlair, userLine, userRating, userTitle, profileUrl } from 'lib/view/userLink';

import type { SimplePlayer } from '../interfaces';

export const player = (
  p: SimplePlayer,
  asLink: boolean,
  withRating: boolean,
  defender = false,
  leader = false,
) => {
  const profileHref = profileUrl(p.name);
  return h(
    'a.ulpt.user-link.online' + (((p.title || '') + p.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink || 'ontouchstart' in window ? { href: profileHref } : { 'data-href': profileHref },
      hook: { destroy: vnode => $.powerTip.destroy(vnode.elm) },
    },
    [
      h(
        'span.name' + (defender ? '.defender' : leader ? '.leader' : ''),
        defender ? { attrs: dataIcon(licon.Shield) } : leader ? { attrs: dataIcon(licon.Crown) } : {},
        [p.patronColor && userLine({ patronColor: p.patronColor }), ...fullName(p)],
      ),
      withRating ? h('span.rating', userRating({ ...p, brackets: false })) : null,
    ],
  );
};

export const fullName = (p: LightUserNoId & { realName?: string }): MaybeVNodes => [
  userTitle(p),
  ...(p.realName ? [p.realName, h('br'), h('span.username-low', `(${p.name})`)] : [p.name]),
  userFlair(p),
];
