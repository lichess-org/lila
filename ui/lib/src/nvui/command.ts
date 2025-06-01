import { type VNode, type VNodeChildren, h } from 'snabbdom';
import { renderPieceKeys, renderPiecesOn, type MoveStyle } from './chess';
import type { Pieces } from '@lichess-org/chessground/types';
import { noTrans } from '../snabbdom';

interface Command {
  help: VNode;
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
  h('h2', `${i18n.nvui.boardCommandList}`),
  h('p', [
    `i: ${i18n.nvui.goToInputForm}`,
    ...[
      `o: ${i18n.nvui.announceCurrentSquare}`,
      `c: ${i18n.nvui.announceLastMoveCapture}`,
      `l: ${i18n.nvui.announceLastMove}`,
      `t: ${i18n.keyboardMove.readOutClocks}`,
      `m: ${i18n.nvui.announcePossibleMoves}`,
      `shift+m: ${i18n.nvui.announcePossibleCaptures}`,
      `arrow keys: ${i18n.nvui.moveWithArrows}`,
      `k-q-r-b-n-p: ${i18n.nvui.moveToPieceByType}`,
      `1-8: ${i18n.nvui.moveToRank}`,
      `Shift+1-8: ${i18n.nvui.moveToFile}`,
      `Shift+a/d: ${i18n.site.keyMoveBackwardOrForward}`,
      `Alt+Shift+a/d: ${i18n.site.cyclePreviousOrNextVariation}`,
    ].reduce(addBreaks, []),
  ]),
];

export const addBreaks = (acc: VNodeChildren[], strOrVNode: string | VNode): VNodeChildren[] =>
  acc.concat([h('br'), strOrVNode]);
