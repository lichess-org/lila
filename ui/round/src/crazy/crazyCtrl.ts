import { dragNewPiece } from '@lichess-org/chessground/drag';
import { setDropMode, cancelDropMode } from '@lichess-org/chessground/drop';
import type { MouchEvent } from '@lichess-org/chessground/types';
import { allKeys } from '@lichess-org/chessground/util';

import { isPlayerTurn } from 'lib/game';
import { pubsub } from 'lib/pubsub';
import { storage } from 'lib/storage';

import type RoundController from '../ctrl';
import type { RoundData } from '../interfaces';

export const pieceRoles: Exclude<Role, 'king'>[] = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

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
  // Only clear a pending click-selection of a *different* piece here. mousedown
  // always fires before the paired click event, so clearing on every mousedown
  // (even for the already-selected piece) would make click()'s own toggle-off
  // immediately re-arm the same piece instead of deselecting it.
  if (clickedPiece && (clickedPiece.color !== color || clickedPiece.role !== role)) {
    clickedPiece = undefined;
    refreshDrop(ctrl);
    ctrl.redraw();
  }
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

export let clickedPiece: { color: Color; role: Exclude<Role, 'king'> } | undefined;

export function isClicked(color: Color, role: Role): boolean {
  return clickedPiece?.color === color && clickedPiece?.role === role;
}

export function click(ctrl: RoundController, e: MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
    role = el.getAttribute('data-role') as Exclude<Role, 'king'>,
    color = el.getAttribute('data-color') as Color,
    number = el.getAttribute('data-nb');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  if (isClicked(color, role)) clickedPiece = undefined;
  else if (number !== '0') clickedPiece = { color, role };
  else return;
  crazyKeys.length = 0;
  refreshDrop(ctrl);
  ctrl.redraw();
}

let dropWithKey = false;
let dropWithDrag = false;
let mouseIconsLoaded = false;

function isLegalDropSquare(data: RoundData, role: Role, key: Key): boolean {
  if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

  const dropStr = data.possibleDrops;

  if (typeof dropStr === 'undefined' || dropStr === null) return true;

  const drops = dropStr.match(/.{2}/g);

  return !!drops?.includes(key);
}

export function valid(data: RoundData, role: Role, key: Key): boolean {
  if (crazyKeys.length === 0) dropWithDrag = true;
  else {
    dropWithKey = true;
    if (!mouseIconsLoaded) preloadMouseIcons(data);
  }

  if (!isPlayerTurn(data)) return false;

  return isLegalDropSquare(data, role, key);
}

// Squares the armed piece could legally land on, for board highlighting.
function dropDests(ctrl: RoundController, role: Exclude<Role, 'king'>): Key[] {
  if (!isPlayerTurn(ctrl.data)) return [];
  const pieces = ctrl.chessground.state.pieces;
  return allKeys.filter(key => !pieces.has(key) && isLegalDropSquare(ctrl.data, role, key));
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

let activeCursor: string | undefined;

function activeRole(): Exclude<Role, 'king'> | undefined {
  return crazyKeys.length > 0 ? pieceRoles[crazyKeys[crazyKeys.length - 1] - 1] : clickedPiece?.role;
}

// chessground's dropmode stays armed with the same piece after a completed
// drop (that's what makes the keyboard shortcut sticky while held), so a
// click-selected drop must explicitly disarm itself once used.
export function onDrop(ctrl: RoundController): void {
  if (clickedPiece) {
    clickedPiece = undefined;
    refreshDrop(ctrl);
    ctrl.redraw();
  }
}

export function refreshDrop(ctrl: RoundController): void {
  if (activeCursor) document.body.classList.remove(activeCursor);
  const role = activeRole();
  const state = ctrl.chessground.state;
  if (role) {
    const color = ctrl.data.player.color,
      crazyData = ctrl.data.crazyhouse;
    if (!crazyData) return;

    const nb = crazyData.pockets[color === 'white' ? 0 : 1][role];
    setDropMode(state, nb ? { color, role } : undefined);
    activeCursor = `cursor-${color}-${role}`;
    document.body.classList.add(activeCursor);
    state.highlight.custom =
      nb && ctrl.data.pref.destination && !ctrl.blindfold()
        ? new Map(dropDests(ctrl, role).map(key => [key, 'move-dest']))
        : undefined;
  } else {
    cancelDropMode(state);
    activeCursor = undefined;
    state.highlight.custom = undefined;
  }
  ctrl.chessground.redrawAll();
}

export function init(ctrl: RoundController): void {
  const k = site.mousetrap;

  const setDrop = () => refreshDrop(ctrl);

  // This case is needed if the pocket piece becomes available while
  // the corresponding drop key is active.
  //
  // When the drop key is first pressed, the cursor will change, but
  // chessground.setDropMove(state, undefined) is called, which means
  // clicks on the board will not drop a piece.
  // If the piece becomes available, we call into chessground again.
  //
  // A completed click-selected drop (clickedPiece) is intentionally not
  // reapplied here, so it clears itself after a single drop.
  pubsub.on('ply', () => {
    clickedPiece = undefined;
    refreshDrop(ctrl);
  });

  for (let i = 1; i <= 5; i++) {
    const iStr = i.toString();
    k.bind(iStr, () => {
      if (!crazyKeys.includes(i)) {
        crazyKeys.push(i);
        clickedPiece = undefined;
        setDrop();
      }
    }).bind(
      iStr,
      () => {
        const idx = crazyKeys.indexOf(i);
        if (idx !== -1) {
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
      if ((e.target as HTMLElement).localName === 'input') resetKeys();
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
  for (const pKey of 'PNBRQ') fetch(site.asset.url(`piece/cburnett-cursor/${colorKey}${pKey}.svg`));
  mouseIconsLoaded = true;
}
