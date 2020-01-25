import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { User } from './interfaces'

export function userIcon(user: User, cls: string): VNode {
  return h('div.user-link.' + cls, {
    class: {
      online: user.online,
      offline: !user.online
    }
  }, [
    h('i.line' + (user.patron ? '.patron' : ''))
  ]);
}

export function userName(user: User): Array<string | VNode> {
  return user.title ? [
    h(
      'span.title',
      user.title == 'BOT' ? { attrs: {'data-bot': true } } : {},
      user.title
    ), ' ', user.name
  ] : [user.name];
}

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        e.stopPropagation();
        f(e);
        return false;
      });
    }
  };
}

export function bindMobileMousedown(f: (e: Event) => any) {
  return {
    insert: (vnode: VNode) => {
      for (const eventName of ['touchstart', 'mousedown'])
        (vnode.elm as HTMLElement).addEventListener(eventName, e => {
          e.stopPropagation();
          f(e);
          return false;
        });
    }
  };
}
