import { h } from 'snabbdom';

import { Player } from '../interfaces';
import SimulCtrl from '../ctrl';
import { i18nVdom } from 'i18n';

export function player(p: Player, ctrl: SimulCtrl) {
  return h(
    'a.text.ulpt.user-link.' + (ctrl.data.host.id != p.id ? 'online' : 'offline'),
    {
      attrs: { href: '/@/' + p.id },
      hook: {
        destroy(vnode) {
          $.powerTip.destroy(vnode.elm as HTMLElement);
        },
      },
    },
    [
      h(`i.line${p.patron ? '.patron' : ''}`),
      h('span.name', userName(p)),
      h('em', ` ${p.rating}${p.provisional ? '?' : ''}`),
    ]
  );
}

const userName = (u: LightUser) => (u.title ? [h('span.utitle', u.title), ' ' + u.name] : [u.name]);

export const title = (ctrl: SimulCtrl) =>
  h('h1', [
    ctrl.data.fullName,
    h('br'),
    h('span.author', i18nVdom('by', player(ctrl.data.host, ctrl))),
  ]);
