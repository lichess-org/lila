import { h } from 'snabbdom'

export function userLink(u: string) {
  const split = u.split(' ');
  return h('a', {
    class: {
      user_link: true,
      ulpt: true
    },
    attrs: {
      href: '/@/' + (split.length == 1 ? split[0] : split[1])
    }
  }, u.substring(0, 14));
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
