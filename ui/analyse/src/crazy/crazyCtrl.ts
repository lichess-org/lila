import { dragNewPiece } from '@lichess-org/chessground/drag';
import { setDropMode, cancelDropMode } from '@lichess-org/chessground/drop';
import type { MouchEvent } from '@lichess-org/chessground/types';

import type AnalyseCtrl from '@/ctrl';

export function drag(ctrl: AnalyseCtrl, color: Color, e: MouchEvent): void {
  const { chessground } = ctrl;
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (chessground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as Role,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  const piece = { color, role };
  const dm = chessground.state.dropmode;
  const sameSelected = !!dm.active && dm.piece?.role === role && dm.piece?.color === color;
  if (sameSelected) cancelDropMode(chessground.state);
  else setDropMode(chessground.state, piece);
  ctrl.redraw();
  dragNewPiece(chessground.state, piece, e);
}

export function valid(chessground: CgApi, possibleDrops: Key[] | undefined, piece: Piece, pos: Key): boolean {
  if (piece.color !== chessground.state.movable.color) return false;
  if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;
  return !possibleDrops || possibleDrops.includes(pos);
}
