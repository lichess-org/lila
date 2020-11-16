import { State } from './state'
import * as cg from './types'
import * as board from './board'
import * as util from './util'
import { cancel as dragCancel } from './drag'

export function setDropMode(s: State, piece?: cg.Piece): void {
  s.dropmode = {
    active: true,
    piece
  };
  dragCancel(s);
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
    s.pieces.set('a0', piece);
    const position = util.eventPosition(e);
    const dest = position && board.getKeyAtDomPos(
      position, board.whitePov(s), s.dom.bounds());
    if (dest) board.dropNewPiece(s, 'a0', dest);
  }
  s.dom.redraw();
}
