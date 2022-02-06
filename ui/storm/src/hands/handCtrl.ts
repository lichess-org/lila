import { dragNewPiece } from 'shogiground/drag';
import { setDropMode, cancelDropMode } from 'shogiground/drop';
import * as cg from 'shogiground/types';
import { Shogi } from 'shogiops/shogi';
import { parseSfen } from 'shogiops/sfen';
import StormCtrl from '../ctrl';
import { parseSquare } from 'shogiops';
import { pretendItsSquare } from 'common';

export function shadowDrop(ctrl: StormCtrl, e: cg.MouchEvent): void {
  const el = e.target as HTMLElement;
  const role = (el.getAttribute('data-role') ?? el.firstElementChild!.getAttribute('data-role')) as cg.Role;
  const color = (el.getAttribute('data-color') ?? el.firstElementChild!.getAttribute('data-color')) as cg.Color;
  const sg = ctrl.ground();
  if (!sg) return;
  const curPiece = sg.state.drawable.piece;
  if (curPiece && curPiece.role == role && curPiece.color == color) sg.state.drawable.piece = undefined;
  else sg.state.drawable.piece = { role: role, color: color };
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

export function drag(ctrl: StormCtrl, e: cg.MouchEvent): void {
  if ((e.button !== undefined && e.button !== 0) || !ctrl.ground()) return; // only touch or left click
  const el = e.target as HTMLElement,
    role = el.getAttribute('data-role') as cg.Role,
    color = el.getAttribute('data-color') as cg.Color,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  !!ctrl.withGround(g => {
    dragNewPiece(g.state, { color, role }, e);
  });
}

export function selectToDrop(ctrl: StormCtrl, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute('data-role') as cg.Role,
    number = el.getAttribute('data-nb'),
    color = el.getAttribute('data-color') as cg.Color;
  if (!role || number === '0') return;
  ctrl.withGround(g => {
    if (g.state.movable.color !== color) return;
    if (!g.state.dropmode.piece || g.state.dropmode.piece.role !== role) {
      ctrl.vm.dropRedraw = true;
      setDropMode(g.state, { color, role });
    } else {
      cancelDropMode(g.state);
      ctrl.vm.dropRedraw = false;
    }
    e.stopPropagation();
    e.preventDefault();
    ctrl.redraw();
  });
}

export function valid(sfen: string, piece: cg.Piece, key: Key): boolean {
  const setup = parseSfen(sfen).unwrap();
  const shogi = Shogi.fromSetup(setup, false);
  return shogi.unwrap(
    s => {
      return s.isLegal({
        role: piece.role,
        to: parseSquare(pretendItsSquare(key))!,
      });
    },
    _ => false
  );
}
