import { dragNewPiece } from 'chessground/drag';
import { readDrops } from 'chess';
import type AnalyseCtrl from '../ctrl';
import type { MouchEvent } from 'chessground/types';

export function drag(ctrl: AnalyseCtrl, color: Color, e: MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.chessground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as Role,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

export function valid(
  chessground: CgApi,
  possibleDrops: string | undefined | null,
  piece: Piece,
  pos: Key,
): boolean {
  if (piece.color !== chessground.state.movable.color) return false;

  if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

  const drops = readDrops(possibleDrops);

  if (drops === null) return true;

  return drops.includes(pos);
}
