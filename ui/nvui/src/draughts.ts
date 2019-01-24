import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import { GameData } from 'game';
import { Pieces } from 'draughtsground/types';
import { Setting, makeSetting } from './setting';

export type Style = 'notation' | 'short' | 'full';

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['notation', 'Notation: 32-27, 18x29'],
      ['short', 'Short: 32 27, 18 takes 29'],
      ['full', 'Full: 32 to 27, 18 takes 29']
    ],
    default: 'full',
    storage: window.lidraughts.storage.make('nvui.moveNotation')
  });
}

function rolePlural(r: String, c: number) {
  if (r === 'man') return c > 1 ? 'men' : 'man';
  else return c > 1 ? r + 's' : r;
}

export function renderSan(san: San, style: Style) {
  if (!san) return ''
  else if (style === 'notation') return san;

  const lowerSan = san.toLowerCase(),
    isCapture = lowerSan.toLowerCase().indexOf('x') >= 0,
    fields = lowerSan.split(isCapture ? 'x' : '-');
  if (fields.length <= 1) return san;

  if (style === 'short') {
    if (isCapture) return [fields[0], 'takes', ...fields.slice(1)].join(' ');
    else return fields.join(' ');
  }
  return [fields[0], isCapture ? 'takes' : 'to', ...fields.slice(1)].join(' ');
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
