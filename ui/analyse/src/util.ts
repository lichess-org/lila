import { fixCrazySan } from 'chess';

import * as m from 'mithril';

// delete me
export function bindOnce(eventName: string, f: (e: Event) => void): Mithril.Config {
  const withRedraw = function(e: Event) {
    m.startComputation();
    f(e);
    m.endComputation();
  };
  return function(el: Element, isUpdate: boolean, ctx: any) {
    if (isUpdate) return;
    el.addEventListener(eventName, withRedraw)
    ctx.onunload = function() {
      el.removeEventListener(eventName, withRedraw);
    };
  }
}

// snabbdom version
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
