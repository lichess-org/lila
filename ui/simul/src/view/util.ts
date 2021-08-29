import { h } from 'snabbdom';
import { Player } from '../interfaces';
import SimulCtrl from '../ctrl';

export function player(p: Player) {
  return h(
    'a.ulpt.user-link.online',
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
      h('em', ` ${p.rating}${p.provisional ? '?' : ''}`),
    ]
  );
}

const userName = (u: LightUser) => (u.title ? [h('span.utitle', u.title), ' ' + u.name] : [u.name]);

export function title(ctrl: SimulCtrl) {
  return h('h1', [ctrl.data.fullName, h('span.author', ctrl.trans.vdom('by', player(ctrl.data.host)))]);
}
