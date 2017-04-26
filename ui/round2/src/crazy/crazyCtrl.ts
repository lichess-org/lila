import { game } from 'game';
import { dragNewPiece } from 'chessground/drag';

export function drag(ctrl, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !game.isPlayerPlaying(ctrl.data)) return;
  var role = e.target.getAttribute('data-role'),
    color = e.target.getAttribute('data-color'),
    number = e.target.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color: color, role: role }, e);
}

export function valid(data, role, key) {

    if (!game.isPlayerTurn(data)) return false;

    if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

    var dropStr = data.possibleDrops;

    if (typeof dropStr === 'undefined' || dropStr === null) return true;

    var drops = dropStr.match(/.{2}/g) || [];

    return drops.indexOf(key) !== -1;
  }
