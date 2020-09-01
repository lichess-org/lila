import { dragNewPiece } from 'shogiground/drag';
import { readDrops } from 'chess';
import AnalyseCtrl from '../ctrl';
import * as cg from 'shogiground/types';
import { Api as ShogigroundApi } from 'shogiground/api';

export function drag(ctrl: AnalyseCtrl, color: Color, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.shogiground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as cg.Role,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.shogiground.state, { color, role }, e);
}

export function valid(shogiground: ShogigroundApi, possibleDrops: string | undefined | null, piece: cg.Piece, pos: Key): boolean {

  if (piece.color !== shogiground.state.movable.color) return false;

  if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '9')) return false; //todo

  const drops = readDrops(possibleDrops);

  if (drops === null) return true;

  return drops.includes(pos);
}
