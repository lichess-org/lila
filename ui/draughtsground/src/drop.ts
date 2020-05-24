import { State } from './state'
import * as cg from './types'
import * as board from './board'
import * as util from './util'
import { cancel as cancelDrag } from './drag'

export function setDropMode(s: State, piece?: cg.Piece): void {
  s.dropmode = {
    active: true,
    piece
  };
  cancelDrag(s);
}

export function cancelDropMode(s: State): void {
  s.dropmode = {
    active: false
  };
}

export function drop(s: State, e: cg.MouchEvent): void {
  if (!s.dropmode.active) return;

  board.unsetPremove(s);
  board.unsetPredrop(s);

  const piece = s.dropmode.piece;

  if (piece) {
    s.pieces.a0 = piece;
    const position = util.eventPosition(e);
    const dest = position && board.getKeyAtDomPos(
      position, s.boardSize, board.whitePov(s), s.dom.bounds());
    if (dest) board.dropNewPiece(s, '00', dest);
  }
  s.dom.redraw();
}
