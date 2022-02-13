import { isPlayerTurn } from 'game/game';
import { dragNewPiece } from 'chessground-newchess1-mod/drag';
import { setDropMode, cancelDropMode } from 'chessground-newchess1-mod/drop';
import RoundController from '../ctrl';
import * as cg from 'chessground-newchess1-mod/types';
import { RoundData } from '../interfaces';

export const pieceRoles: cg.Role[] = ['doom'];

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

let dropWithKey = false;
let dropWithDrag = false;
let mouseIconsLoaded = false;

export function valid(data: RoundData, role: cg.Role, key: cg.Key): boolean {
  if (newChess1Keys.length === 0) dropWithDrag = true;
  else {
    dropWithKey = true;
    if (!mouseIconsLoaded) preloadMouseIcons(data);
  }

  if (!isPlayerTurn(data)) return false;

  const backrank = data.game.player === 'white' ? '1' : '8';

  if (role === 'doom' && !(key[1] === backrank && (key[0] === 'd' || key[0] === 'e'))) return false;

  const dropStr = data.possibleDrops;

  if (typeof dropStr === 'undefined' || dropStr === null) return true;

  const drops = dropStr.match(/.{2}/g) || [];

  return drops.includes(key);
}

export function onEnd() {
  const store = lichess.storage.make('newChess1KeyHist');
  if (dropWithKey) store.set(10);
  else if (dropWithDrag) {
    const cur = parseInt(store.get()!);
    if (cur > 0 && cur <= 10) store.set(cur - 1);
    else if (cur !== 0) store.set(3);
  }
}

export const newChess1Keys: Array<number> = [];

export function init(ctrl: RoundController) {
  const k = window.Mousetrap;

  let activeCursor: string | undefined;

  const setDrop = () => {
    if (activeCursor) document.body.classList.remove(activeCursor);
    if (newChess1Keys.length > 0) {
      const role = pieceRoles[newChess1Keys[newChess1Keys.length - 1] - 1],
        color = ctrl.data.player.color,
        newChess1 = ctrl.data.newChess1;
      if (!newChess1) return;

      const nb = newChess1.pockets[color === 'white' ? 0 : 1][role];
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
  lichess.pubsub.on('ply', () => {
    if (newChess1Keys.length > 0) setDrop();
  });

  for (let i = 1; i <= 5; i++) {
    const iStr = i.toString();
    k.bind(iStr, () => {
      if (!newChess1Keys.includes(i)) {
        newChess1Keys.push(i);
        setDrop();
      }
    }).bind(
      iStr,
      () => {
        const idx = newChess1Keys.indexOf(i);
        if (idx >= 0) {
          newChess1Keys.splice(idx, 1);
          if (idx === newChess1Keys.length) {
            setDrop();
          }
        }
      },
      'keyup'
    );
  }

  const resetKeys = () => {
    if (newChess1Keys.length > 0) {
      newChess1Keys.length = 0;
      setDrop();
    }
  };

  window.addEventListener('blur', resetKeys);

  // Handle focus on input bars – these will hide keyup events
  window.addEventListener(
    'focus',
    e => {
      if (e.target && (e.target as HTMLElement).localName === 'input') resetKeys();
    },
    { capture: true }
  );

  if (lichess.storage.get('newChess1KeyHist') !== '0') preloadMouseIcons(ctrl.data);
}

// zh keys has unacceptable jank when cursors need to dl,
// so preload when the feature might be used.
// Images are used in _zh.scss, which should be kept in sync.
function preloadMouseIcons(data: RoundData) {
  const colorKey = data.player.color[0];
  for (const pKey of 'PNBRQ') fetch(lichess.assetUrl(`piece/cburnett/${colorKey}${pKey}.svg`));
  mouseIconsLoaded = true;
}
