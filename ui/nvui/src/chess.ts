import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import { GameData } from 'game';
import { Pieces } from 'chessground/types';

export function piecesHtml(pieces: Pieces, style: string): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).map((k: string) => renderKey(k, style)).join(', ')}`
      ).join(', ')
    ]);
  }));
}

const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
export function renderKey(key: string, style: string): string {
  return (style === 'anna' || style === 'full') ? `${anna[key[0]]} ${key[1]}` : key;
}
