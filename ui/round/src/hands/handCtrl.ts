import { isPlayerTurn } from 'game/game';
import { dragNewPiece } from 'shogiground/drag';
import { setDropMode, cancelDropMode } from 'shogiground/drop';
import RoundController from '../ctrl';
import * as cg from 'shogiground/types';
import { lishogiVariantRules } from 'shogiops/compat';
import { handRoles } from 'shogiops/variantUtil';
import { parseSfen, parseHands } from 'shogiops/sfen';
import { setupPosition } from 'shogiops/variant';
import { lastStep } from '../round';
import { parseSquare } from 'shogiops/util';
import { pretendItsSquare } from 'common';

const li = window.lishogi;

export function shadowDrop(ctrl: RoundController, e: cg.MouchEvent): void {
  const el = e.target as HTMLElement;
  const role = (el.getAttribute('data-role') ?? el.firstElementChild!.getAttribute('data-role')) as cg.Role;
  const color = (el.getAttribute('data-color') ?? el.firstElementChild!.getAttribute('data-color')) as cg.Color;
  if (!ctrl.shogiground) return;
  const curPiece = ctrl.shogiground.state.drawable.piece;
  if (curPiece && curPiece.role == role && curPiece.color == color) ctrl.shogiground.state.drawable.piece = undefined;
  else ctrl.shogiground.state.drawable.piece = { role: role, color: color };
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

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
  if (ctrl.dropmodeActive && role !== ctrl.shogiground.state.dropmode.piece?.role) {
    cancelDropMode(ctrl.shogiground.state);
    ctrl.dropmodeActive = false;
    ctrl.redraw();
  }
  dragNewPiece(ctrl.shogiground.state, { color, role }, e);
}

export function selectToDrop(ctrl: RoundController, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
    role = el.getAttribute('data-role') as cg.Role,
    color = el.getAttribute('data-color') as cg.Color,
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  const dropMode = ctrl.shogiground.state.dropmode;
  const dropPiece = ctrl.shogiground.state.dropmode.piece;
  if (!dropMode.active || dropPiece?.role !== role) {
    setDropMode(ctrl.shogiground.state, { color, role });
    ctrl.dropmodeActive = true;
  } else {
    cancelDropMode(ctrl.shogiground.state);
    ctrl.dropmodeActive = false;
  }
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

let dropWithKey = false;
let dropWithDrag = false;

export function valid(ctrl: RoundController, role: cg.Role, key: cg.Key): boolean {
  const data = ctrl.data;
  const lastStep = data.steps[data.steps.length - 1];
  const move = { role: role, to: parseSquare(pretendItsSquare(key))! };

  if (handKeys.length === 0) dropWithDrag = true;
  else dropWithKey = true;

  if (!isPlayerTurn(data)) return false;

  const pos = parseSfen(lastStep.sfen).chain(s => setupPosition(lishogiVariantRules(data.game.variant.key), s, false));
  if (pos.isErr) return false;
  return pos.value.isLegal(move);
}

export function onEnd() {
  const store = li.storage.make('handKeyHist');
  if (dropWithKey) store.set(10);
  else if (dropWithDrag) {
    const cur = parseInt(store.get()!);
    if (cur > 0 && cur <= 10) store.set(cur - 1);
    else if (cur !== 0) store.set(3);
  }
}

export const handKeys: Array<number> = [];

export function init(ctrl: RoundController) {
  const k = window.Mousetrap;

  let activeCursor: string | undefined;

  const setDrop = () => {
    if (activeCursor) document.body.classList.remove(activeCursor);
    if (handKeys.length > 0) {
      const role = handRoles(lishogiVariantRules(ctrl.data.game.variant.key)).reverse()[
          handKeys[handKeys.length - 1] - 1
        ],
        color = ctrl.data.player.color,
        parsedHands = parseHands(lastStep(ctrl.data).sfen.split(' ')[2] || '-');
      if (parsedHands.isErr) return;

      const nb = parsedHands.value[color][role];
      setDropMode(ctrl.shogiground.state, nb > 0 ? { color, role } : undefined);
      activeCursor = `cursor-${color}-${role}`;
      document.body.classList.add(activeCursor);
    } else {
      cancelDropMode(ctrl.shogiground.state);
      activeCursor = undefined;
    }
  };

  // This case is needed if the pocket piece becomes available while
  // the corresponding drop key is active.
  //
  // When the drop key is first pressed, the cursor will change, but
  // shogiground.setDropMove(state, undefined) is called, which means
  // clicks on the board will not drop a piece.
  // If the piece becomes available, we call into shogiground again.
  window.lishogi.pubsub.on('ply', () => {
    if (handKeys.length > 0) setDrop();
  });

  for (let i = 1; i <= handRoles(lishogiVariantRules(ctrl.data.game.variant.key)).length; i++) {
    const iStr = i.toString();
    k.bind(iStr, () => {
      if (!handKeys.includes(i)) {
        handKeys.push(i);
        setDrop();
      }
    }).bind(
      iStr,
      () => {
        const idx = handKeys.indexOf(i);
        if (idx >= 0) {
          handKeys.splice(idx, 1);
          if (idx === handKeys.length) {
            setDrop();
          }
        }
      },
      'keyup'
    );
  }

  const resetKeys = () => {
    if (handKeys.length > 0) {
      handKeys.length = 0;
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
}
