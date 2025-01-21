import { type VNode, h } from 'snabbdom';
import { renderPieceKeys, renderPiecesOn, type MoveStyle } from './chess';
import type { Pieces } from 'chessground/types';

export const commands = {
  piece: {
    help: 'p: Read locations of a piece type. Example: p N, p k.',
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^p ([pnbrqk])$/i, p => renderPieceKeys(pieces, p, style));
    },
  },
  scan: {
    help: 's: Read pieces on a rank or file. Example: s a, s 1.',
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^s ([a-h1-8])$/i, p => renderPiecesOn(pieces, p, style));
    },
  },
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  if (!c.match(regex)) return undefined;
  return f(c.replace(regex, '$1'));
}

export const boardCommands = (): VNode[] => [
  h('h2', 'Board mode commands'),
  h('p', [
    'Use these commands when focused on the board itself.',
    h('br'),
    'o: announce current position.',
    h('br'),
    "c: announce last move's captured piece.",
    h('br'),
    'l: announce last move.',
    h('br'),
    `t: ${i18n.keyboardMove.readOutClocks}`,
    h('br'),
    'm: announce possible moves for the selected piece.',
    h('br'),
    'shift+m: announce possible moves for the selected pieces which capture..',
    h('br'),
    'arrow keys: move left, right, up or down.',
    h('br'),
    'kqrbnp/KQRBNP: move forward/backward to a piece.',
    h('br'),
    '1-8: move to rank 1-8.',
    h('br'),
    'Shift+1-8: move to file a-h.',
    h('br'),
  ]),
];
