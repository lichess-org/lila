import { dragNewPiece } from 'shogiground/drag';
import { setDropMode, cancelDropMode } from 'shogiground/drop';
import * as cg from 'shogiground/types';
import { Shogi } from 'shogiops';
import { parseChessSquare } from 'shogiops/compat';
import { parseFen } from 'shogiops/fen';
import { PocketRole } from 'shogiops/types';
import { Controller } from '../interfaces';

export function shadowDrop(ctrl: Controller, color: Color, e: cg.MouchEvent): void {
  const el = e.target as HTMLElement;
  const role = (el.getAttribute('data-role') ?? el.firstElementChild!.getAttribute('data-role')) as cg.Role;
  const sg = ctrl.ground();
  if (!sg) return;
  const curPiece = sg.state.drawable.piece;
  if (curPiece && curPiece.role == role && curPiece.color == color) sg.state.drawable.piece = undefined;
  else sg.state.drawable.piece = { role: role, color: color };
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

export function drag(ctrl: Controller, e: cg.MouchEvent): void {
  if ((e.button !== undefined && e.button !== 0) || !ctrl.ground()) return; // only touch or left click
  const el = e.target as HTMLElement,
    role = el.getAttribute('data-role') as cg.Role,
    color = el.getAttribute('data-color') as cg.Color,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.ground()!.state, { color, role }, e);
}

export function selectToDrop(ctrl: Controller, color: Color, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return;
  if (ctrl.ground()!.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as cg.Role,
    number = el.getAttribute('data-nb');
  const sg = ctrl.ground();
  if (!role || number === '0' || !sg) return;
  if (!sg.state.dropmode.piece || sg.state.dropmode.piece.role !== role) {
    ctrl.vm.dropmodeActive = true;
    setDropMode(sg.state, { color, role });
  } else {
    cancelDropMode(sg.state);
    ctrl.vm.dropmodeActive = false;
  }
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

export function valid(fen: string, piece: cg.Piece, pos: Key): boolean {
  const setup = parseFen(fen).unwrap();
  const shogi = Shogi.fromSetup(setup, false);
  return shogi.unwrap(
    s => {
      return s.isLegal({
        role: piece.role as PocketRole,
        to: parseChessSquare(pos)!,
      });
    },
    _ => false
  );
}
