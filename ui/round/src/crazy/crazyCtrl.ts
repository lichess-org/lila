import { isPlayerTurn } from 'game/game';
import { dragNewPiece } from 'chessground/drag';
import RoundController from '../ctrl';
import * as cg from 'chessground/types';
import { RoundData } from '../interfaces';

export function drag(ctrl: RoundController, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
  role = el.getAttribute('data-role') as cg.Role,
  color = el.getAttribute('data-color') as cg.Color,
  number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

export function valid(data: RoundData, role: cg.Role, key: cg.Key): boolean {

  if (!isPlayerTurn(data)) return false;

  if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

  const dropStr = data.possibleDrops;

  if (typeof dropStr === 'undefined' || dropStr === null) return true;

  const drops = dropStr.match(/.{2}/g) || [];

  return drops.indexOf(key) !== -1;
}
