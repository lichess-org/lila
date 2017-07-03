import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return {
    insert: vnode => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (redraw) redraw();
        return res;
      });
    }
  };
}

export function dataIcon(icon: string) {
  return {
    'data-icon': icon
  };
}

export function innerHTML(html: string): Hooks {
  return {
    insert: vnode => (vnode.elm as HTMLElement).innerHTML = html,
    postpatch: (_, vnode) => (vnode.elm as HTMLElement).innerHTML = html
  };
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
