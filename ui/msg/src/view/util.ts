import { h, VNode } from 'snabbdom';
import { User } from '../interfaces';

export function userIcon(user: User, cls: string): VNode {
  return h(
    'div.user-link.' + cls,
    {
      class: {
        online: user.online,
        offline: !user.online,
      },
    },
    [h('i.line' + (user.patron ? '.patron' : ''))]
  );
}

export const userName = (user: User): Array<string | VNode> =>
  user.title
    ? [h('span.utitle', user.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, user.title), ' ', user.name]
    : [user.name];

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        e.stopPropagation();
        f(e);
        return false;
      });
    },
  };
}

export function bindMobileMousedown(f: (e: Event) => any) {
  return bind('ontouchstart' in window ? 'click' : 'mousedown', f);
}

export function spinner(): VNode {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' },
      }),
    ]),
  ]);
}
