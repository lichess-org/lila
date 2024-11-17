import { isPlayerTurn } from 'game/game';
import { dragNewPiece } from 'chessground/drag';
import { setDropMode, cancelDropMode } from 'chessground/drop';
import type RoundController from '../ctrl';
import type { MouchEvent } from 'chessground/types';
import type { RoundData } from '../interfaces';
import { storage } from 'common/storage';
import { pubsub } from 'common/pubsub';

export const pieceRoles: Role[] = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

export function drag(ctrl: RoundController, e: MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
    role = el.getAttribute('data-role') as Role,
    color = el.getAttribute('data-color') as Color,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

let dropWithKey = false;
let dropWithDrag = false;
let mouseIconsLoaded = false;

export function valid(data: RoundData, role: Role, key: Key): boolean {
  if (crazyKeys.length === 0) dropWithDrag = true;
  else {
    dropWithKey = true;
    if (!mouseIconsLoaded) preloadMouseIcons(data);
  }

  if (!isPlayerTurn(data)) return false;

  if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

  const dropStr = data.possibleDrops;

  if (typeof dropStr === 'undefined' || dropStr === null) return true;

  const drops = dropStr.match(/.{2}/g);

  return drops?.includes(key) === true;
}

export function onEnd(): void {
  const store = storage.make('crazyKeyHist');
  if (dropWithKey) store.set(10);
  else if (dropWithDrag) {
    const cur = parseInt(store.get()!);
    if (cur > 0 && cur <= 10) store.set(cur - 1);
    else if (cur !== 0) store.set(3);
  }
}

export const crazyKeys: Array<number> = [];

export function init(ctrl: RoundController): void {
  const k = site.mousetrap;

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
  pubsub.on('ply', () => {
    if (crazyKeys.length > 0) setDrop();
  });

  for (let i = 1; i <= 5; i++) {
    const iStr = i.toString();
    k.bind(iStr, () => {
      if (!crazyKeys.includes(i)) {
        crazyKeys.push(i);
        setDrop();
      }
    }).bind(
      iStr,
      () => {
        const idx = crazyKeys.indexOf(i);
        if (idx >= 0) {
          crazyKeys.splice(idx, 1);
          if (idx === crazyKeys.length) {
            setDrop();
          }
        }
      },
      'keyup',
    );
  }

  const resetKeys = () => {
    if (crazyKeys.length > 0) {
      crazyKeys.length = 0;
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
    { capture: true },
  );

  if (storage.get('crazyKeyHist') !== '0') preloadMouseIcons(ctrl.data);
}

// zh keys has unacceptable jank when cursors need to dl,
// so preload when the feature might be used.
// Images are used in _zh.scss, which should be kept in sync.
function preloadMouseIcons(data: RoundData) {
  const colorKey = data.player.color[0];
  for (const pKey of 'PNBRQ') fetch(site.asset.url(`piece/cburnett/${colorKey}${pKey}.svg`));
  mouseIconsLoaded = true;
}
