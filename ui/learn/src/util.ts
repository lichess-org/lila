import type { SquareName as Key } from 'chessops';
import { Level, LevelPartial } from './stage/list';
import { h } from 'snabbdom';
import * as cg from 'chessground/types';
import { DrawShape } from 'chessground/draw';

export type WithGround = <A>(f: (cg: CgApi) => A) => A | false | undefined;

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

export const assetUrl = document.body.dataset.assetUrl + '/assets/';

export type PromotionRole = 'knight' | 'bishop' | 'rook' | 'queen';
export type PromotionChar = 'n' | 'b' | 'r' | 'q';

export const isRole = (str: PromotionChar | PromotionRole): str is PromotionRole => str.length > 1;

export const arrow = (vector: Uci, brush?: cg.BrushColor): DrawShape => ({
  brush: brush || 'paleGreen',
  orig: vector.slice(0, 2) as Key,
  dest: vector.slice(2, 4) as Key,
});

export const circle = (key: Key, brush?: cg.BrushColor): DrawShape => ({
  brush: brush || 'green',
  orig: key,
});

export const readKeys = (keys: string | Key[]): Key[] =>
  typeof keys === 'string' ? (keys.split(' ') as Key[]) : keys;

export const pieceImg = (role: cg.Role) => h('div.no-square', h('piece.white.' + role));

export const roundSvg = (url: string) => h('div.round-svg', h('img', { attrs: { src: url } }));

export const withLinebreaks = (text: string) =>
  text.split(/(\n)/g).map(part => (part === '\n' ? h('br') : part));

export const decomposeUci = (uci: string) =>
  [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)] as [Key, Key, PromotionChar | ''];

export const oppColor = (color: Color): Color => (color == 'white' ? 'black' : 'white');
