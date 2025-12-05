import { h } from 'snabbdom';
import * as licon from 'lib/licon';
import { dataIcon } from 'lib/view';
import { fullName, userLine, userRating } from 'lib/view/userLink';
import type { SimplePlayer } from '../interfaces';

export const player = (
  p: SimplePlayer,
  asLink: boolean,
  withRating: boolean,
  defender = false,
  leader = false,
) =>
  h(
    'a.ulpt.user-link.online' + (((p.title || '') + p.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink || 'ontouchstart' in window ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
      hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
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
