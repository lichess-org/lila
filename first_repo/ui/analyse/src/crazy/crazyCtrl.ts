import { dragNewPiece } from 'chessground/drag';
import { readDrops } from 'chess';
import AnalyseCtrl from '../ctrl';
import * as cg from 'chessground/types';
import { Api as ChessgroundApi } from 'chessground/api';

export function drag(ctrl: AnalyseCtrl, color: Color, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.chessground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as cg.Role,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

export function valid(
  chessground: ChessgroundApi,
  possibleDrops: string | undefined | null,
  piece: cg.Piece,
  pos: Key
): boolean {
  if (piece.color !== chessground.state.movable.color) return false;

  if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

  const drops = readDrops(possibleDrops);

  if (drops === null) return true;

  return drops.includes(pos);
}
