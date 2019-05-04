import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

export function userLink(u: string, title?: string) {
  const trunc = u.substring(0, 14);
  return h('a', {
    // can't be inlined because of thunks
    class: {
      'user-link': true,
      ulpt: true
    },
    attrs: {
      href: '/@/' + u
    }
  }, title ? [
    h(
      'span.title',
      title == 'BOT' ? { attrs: {'data-bot': true } } : {},
      title), trunc
  ] : [trunc]);
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener(eventName, f);
    }
  };
}
