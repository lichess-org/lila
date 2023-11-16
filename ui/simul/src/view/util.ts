import { h } from 'snabbdom';

import { Player } from '../interfaces';
import SimulCtrl from '../ctrl';
import { userFlair } from 'common/userLink';

export function player(p: Player, ctrl: SimulCtrl) {
  return h(
    'a.ulpt.user-link.' + (p.online || ctrl.data.host.id != p.id ? 'online' : 'offline'),
    {
      attrs: { href: '/@/' + p.name },
      hook: {
        destroy(vnode) {
          $.powerTip.destroy(vnode.elm as HTMLElement);
        },
      },
    },
    [
      h(`i.line${p.patron ? '.patron' : ''}`),
      h('span.name', userName(p)),
      ctrl.opts.showRatings ? h('em', ` ${p.rating}${p.provisional ? '?' : ''}`) : null,
    ],
  );
}

const userName = (u: LightUser) =>
  u.title ? [h('span.utitle', u.title), ' ' + u.name, userFlair(u.flair)] : [u.name, userFlair(u.flair)];

export const title = (ctrl: SimulCtrl) => h('h1', ctrl.data.fullName);
