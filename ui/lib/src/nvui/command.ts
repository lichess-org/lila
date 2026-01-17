import { type VNode, type VNodeChildren, h } from 'snabbdom';
import { renderPieceKeys, renderPiecesOn, type MoveStyle } from './chess';
import type { Pieces } from '@lichess-org/chessground/types';
import { memoize } from '@/common';

interface Command {
  help: VNode | string;
  apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined;
}
type Commands = {
  [name: string]: Command;
};

export const commands: () => Commands = memoize(() => ({
  piece: {
    help: i18n.nvui.announcePieceLocations,
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^\/?p ([apnbrqk])$/i, p => renderPieceKeys(pieces, p, style));
    },
  },
  scan: {
    help: i18n.nvui.announcePiecesOnRankOrFile,
    apply(c: string, pieces: Pieces, style: MoveStyle): string | undefined {
      return tryC(c, /^\/?s ([a-h1-8])$/i, p => renderPiecesOn(pieces, p, style));
    },
  },
  board: {
    help: i18n.nvui.goToBoard,
    apply(c: string, _pieces: Pieces, _style: MoveStyle): string {
      const words = c.split(' ');
      const file = words[1]?.charAt(0) || 'e';
      const rank = words[1]?.charAt(1) || '4';
      const button =
        (!words[1] && $('button.active').get(0)) ||
        $('button[file="' + file + '"][rank="' + rank + '"]').get(0);
      if (button) {
        button.focus();
        return '';
      } else {
        return `${file}.${rank} ${i18n.nvui.notAValidSquare}`;
      }
    },
  },
}));

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  return c.match(regex) ? f(c.replace(regex, '$1')) : undefined;
}

export const boardCommands = (): VNode[] => [
  h('h2', i18n.nvui.boardCommandList),
  h('p', [
    `i: ${i18n.nvui.goToInputForm}`,
    ...[
      `o: ${i18n.nvui.announceCurrentSquare}`,
      `c: ${i18n.nvui.announceLastMoveCapture}`,
      `l: ${i18n.nvui.announceLastMove}`,
      `t: ${i18n.keyboardMove.readOutClocks}`,
      `m: ${i18n.nvui.announcePossibleMoves}`,
      `arrow keys: ${i18n.nvui.moveWithArrows}`,
      `k-q-r-b-n-p: ${i18n.nvui.moveToPieceByType}`,
      `1 to 8: ${i18n.nvui.moveToRank}`,
      `shift+1 to 8: ${i18n.nvui.moveToFile}`,
      `shift+a/d: ${i18n.site.keyMoveBackwardOrForward}`,
      `x: ${i18n.nvui.announcePiecesAroundCurrentSquare}`,
      `shift+m: ${i18n.nvui.announcePossibleCaptures}`,
      `v: ${i18n.nvui.announceComputerEvaluation}`,
      `g: ${i18n.nvui.announceComputerBestMove}`,
      `shift+g: ${i18n.nvui.playComputerBestMove}`,
      `alt+shift+a/d: ${i18n.site.cyclePreviousOrNextVariation}`,
    ].reduce(addBreaks, []),
  ]),
];

export const addBreaks = (acc: VNodeChildren[], strOrVNode: string | VNode): VNodeChildren[] =>
  acc.concat([h('br'), strOrVNode]);
