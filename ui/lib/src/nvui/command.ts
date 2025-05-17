import { type VNode, type VNodeChildren, h } from 'snabbdom';
import { renderPieceKeys, renderPiecesOn, type MoveStyle } from './chess';
import type { Pieces } from 'chessground/types';
import { noTrans } from '../snabbdom';

interface Command {
  help: string | VNode;
  apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined;
}
type Commands = {
  [name: string]: Command;
};

export const commands: Commands = {
  piece: {
    help: noTrans('Read locations of a piece type. Example: p N, p k.'),
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^\/?p ([pnbrqk])$/i, p => renderPieceKeys(pieces, p, style));
    },
  },
  scan: {
    help: noTrans('Read pieces on a rank or file. Example: s a, s 1.'),
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^\/?s ([a-h1-8])$/i, p => renderPiecesOn(pieces, p, style));
    },
  },
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  return c.match(regex) ? f(c.replace(regex, '$1')) : undefined;
}

export const boardCommands = (): VNode[] => [
  h('h2', 'Board mode commands'),
  h(
    'p',
    [
      noTrans('Use these commands when focused on the board itself.'),
      noTrans('i: go to move input form.'),
      noTrans('o: announce current position.'),
      noTrans("c: announce last move's captured piece."),
      noTrans('l: announce last move.'),
      `t: ${i18n.keyboardMove.readOutClocks}`,
      noTrans('m: announce possible moves for the selected piece.'),
      noTrans('shift+m: announce possible moves for the selected pieces which capture..'),
      noTrans('arrow keys: move left, right, up or down.'),
      noTrans('kqrbnp/KQRBNP: move forward/backward to a piece.'),
      noTrans('1-8: move to rank 1-8.'),
      noTrans('Shift+1-8: move to file a-h.'),
      `Shift+a/d: ${i18n.site.keyMoveBackwardOrForward}`,
      `Alt+Shift+a/d: ${i18n.site.cyclePreviousOrNextVariation}`,
    ].reduce(addBreaks, []),
  ),
];

export const addBreaks = (acc: VNodeChildren[], str: string): VNodeChildren[] => acc.concat([h('br'), str]);
