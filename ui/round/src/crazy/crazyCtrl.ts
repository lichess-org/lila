import { isPlayerTurn } from 'game/game';
import { dragNewPiece } from 'chessground/drag';
import { setDropMode, cancelDropMode } from 'chessground/drop';
import RoundController from '../ctrl';
import * as cg from 'chessground/types';
import { RoundData } from '../interfaces';

export const pieceRoles: cg.Role[] = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

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

  return drops.includes(key);
}

export const crazyKeys: Array<number> = [];

export function init(ctrl: RoundController) {
  if (!ctrl.data.crazyhouse) return;
  const k = window.Mousetrap;

  let activeCursor: string | undefined;

  const setDrop = () => {
    if (activeCursor) document.body.classList.remove(activeCursor);
    if (crazyKeys.length > 0) {
      const role = pieceRoles[crazyKeys[crazyKeys.length - 1] - 1],
        color = ctrl.data.player.color,
        crazyData = ctrl.data.crazyhouse;
      if (!crazyData) return;

      const nb = crazyData.pockets[color === 'white' ? 0 : 1][role];
      setDropMode(ctrl.chessground.state, nb > 0 ? { color, role } : undefined);
      activeCursor = `cursor-${color}-${role}`;
      document.body.classList.add(activeCursor);
    } else {
      cancelDropMode(ctrl.chessground.state);
      activeCursor = undefined;
    }
  };

  // This case is needed if the pocket piece becomes available while
  // the corresponding drop key is active.
  //
  // When the drop key is first pressed, the cursor will change, but
  // chessground.setDropMove(state, undefined) is called, which means
  // clicks on the board will not drop a piece.
  // If the piece becomes available, we call into chessground again.
  window.lichess.pubsub.on('ply', () => {
    if (crazyKeys.length > 0) setDrop();
  })

  for (let i = 1; i <= 5; i++) {
    const iStr = i.toString();
    k.bind(iStr, (e: KeyboardEvent) => {
      e.preventDefault();
      if (!crazyKeys.includes(i)) {
        crazyKeys.push(i);
        setDrop();
      }
    });
    k.bind(iStr, (e: KeyboardEvent) => {
      e.preventDefault();
      const idx = crazyKeys.indexOf(i);
      if (idx >= 0) {
        crazyKeys.splice(idx, 1);
        if (idx === crazyKeys.length) {
          setDrop();
        }
      }
    }, 'keyup');
  }
}
