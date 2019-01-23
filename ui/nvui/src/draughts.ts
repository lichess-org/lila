import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import { GameData } from 'game';
import { Pieces } from 'draughtsground/types';

function rolePlural(r: String, c: number) {
  if (r === 'man') return c > 1 ? 'men' : 'man';
  else return c > 1 ? r + 's' : r;
}

export function piecesHtml(pieces: Pieces): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'man'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([rolePlural(role, keys.length), ...keys.sort().map(key => key[0] === '0' ? key.slice(1) : key)]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).join(', ')}`
      ).join(', ')
    ]);
  }));
}
