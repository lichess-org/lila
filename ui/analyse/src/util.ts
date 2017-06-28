import { h } from 'snabbdom'
import { fixCrazySan } from 'chess';

export function bind(eventName: string, f: (e: Event) => void, redraw: (() => void) | undefined = undefined) {
  return {
    insert: vnode => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        f(e);
        if (redraw) redraw();
      });
    }
  };
}

export function dataIcon(icon: string) {
  return {
    'data-icon': icon
  };
}

export function iconTag(icon: string) {
  return h('i', { attrs: dataIcon(icon) });
}

export function plyToTurn(ply: number): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function synthetic(data): boolean {
  return data.game.id === 'synthetic';
}

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (
    node.ply % 2 === 1 ? '.' : '...'
  ) + ' ' + fixCrazySan(node.san);
  return 'Initial position';
}

export function plural(noun: string, nb: number): string {
  return nb + ' ' + (nb === 1 ? noun : noun + 's');
}

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  const name = split.length == 1 ? split[0] : split[1];
  return name.toLowerCase();
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
export function autolink(str: string, callback: (str: string) => string): string {
  const pattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
  return str.replace(pattern, function(_, space, url) {
    return "" + space + callback(url);
  });
}
