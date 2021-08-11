import type { Square as Key } from 'chess.js';
import m from './mithrilFix';
import { Level, LevelPartial } from './stage/list';

export function toLevel(l: LevelPartial, it: number): Level {
  if (l.fen.split(' ').length === 4) l.fen += ' 0 1';

  return {
    id: it + 1,
    apples: [],
    color: / w /.test(l.fen) ? 'white' : 'black',
    detectCapture: l.apples ? false : 'unprotected',
    ...l,
  };
}

export const assetUrl = $('body').data('asset-url') + '/assets/';

export const roleToSan: {
  [R in PromotionRole]: PromotionChar;
} = {
  knight: 'n',
  bishop: 'b',
  rook: 'r',
  queen: 'q',
};
export type PromotionRole = 'knight' | 'bishop' | 'rook' | 'queen';
export type PromotionChar = 'n' | 'b' | 'r' | 'q';

export function isRole(str: PromotionChar | PromotionRole): str is PromotionRole {
  return str.length > 1;
}

export function arrow(vector: Uci, brush?: string) {
  return {
    brush: brush || 'paleGreen',
    orig: vector.slice(0, 2) as Key,
    dest: vector.slice(2, 4) as Key,
  };
}

export function circle(key: Key, brush?: string) {
  return {
    brush: brush || 'green',
    orig: key,
  };
}

export function readKeys(keys: string | Key[]) {
  return typeof keys === 'string' ? (keys.split(' ') as Key[]) : keys;
}

export function setFenTurn(fen: string, turn: 'b' | 'w') {
  return fen.replace(/ (w|b) /, ' ' + turn + ' ');
}

export function pieceImg(role: string) {
  return m('div.is2d.no-square', m('piece.white.' + role));
}

export function roundSvg(url: string) {
  return m(
    'div.round-svg',
    m('img', {
      src: url,
    })
  );
}

export function withLinebreaks(text: string) {
  return m.trust(lichess.escapeHtml(text).replace(/\n/g, '<br>'));
}

export function decomposeUci(uci: string) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)] as [Key, Key, PromotionChar | ''];
}
