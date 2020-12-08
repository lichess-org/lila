import { isPlayerTurn } from "game/game";
import { dragNewPiece } from "shogiground/drag";
import { setDropMode, cancelDropMode } from "shogiground/drop";
import RoundController from "../ctrl";
import * as cg from "shogiground/types";
import { RoundData } from "../interfaces";

const li = window.lishogi;

export const pieceRoles: cg.Role[] = [
  "pawn",
  "lance",
  "knight",
  "silver",
  "gold",
  "bishop",
  "rook",
];

export function drag(ctrl: RoundController, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
    role = el.getAttribute("data-role") as cg.Role,
    color = el.getAttribute("data-color") as cg.Color,
    number = el.getAttribute("data-nb");
  if (!role || !color || number === "0") return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.shogiground.state, { color, role }, e);
}

let dropWithKey = false;
let dropWithDrag = false;
let mouseIconsLoaded = false;

export function valid(
  data: RoundData,
  ctrl: RoundController,
  role: cg.Role,
  key: cg.Key
): boolean {
  if (crazyKeys.length === 0) dropWithDrag = true;
  else {
    dropWithKey = true;
    if (!mouseIconsLoaded) preloadMouseIcons(data);
  }

  if (!isPlayerTurn(data)) return false;

  const color = ctrl.ply % 2 === 0 ? "white" : "black";

  // You can't place pawn on a file where you already have a pawn
  if (role === "pawn") {
    for (const [k, v] of ctrl.shogiground.state.pieces.entries()) {
      if (
        v.role === "pawn" &&
        v.color === color &&
        key[0] === k[0] &&
        key != k
      ) {
        return false;
      }
    }
  }
  if (
    (role === "pawn" || role === "lance") &&
    ((key[1] === "1" && color === "black") ||
      (key[1] === "9" && color === "white"))
  )
    return false;
  if (
    role === "knight" &&
    (((key[1] === "1" || key[1] === "2") && color === "black") ||
      ((key[1] === "9" || key[1] === "8") && color === "white"))
  )
    return false;
  
  const dropStr = data.possibleDrops;

  if (typeof dropStr === "undefined" || dropStr === null) return true;

  const drops = dropStr.match(/.{2}/g) || [];

  return drops.includes(key);
}

export function onEnd() {
  const store = li.storage.make("crazyKeyHist");
  if (dropWithKey) store.set(10);
  else if (dropWithDrag) {
    const cur = parseInt(store.get()!);
    if (cur > 0 && cur <= 10) store.set(cur - 1);
    else if (cur !== 0) store.set(3);
  }
}

export const crazyKeys: Array<number> = [];

export function init(ctrl: RoundController) {
  const k = window.Mousetrap;

  let activeCursor: string | undefined;

  const setDrop = () => {
    if (activeCursor) document.body.classList.remove(activeCursor);
    if (crazyKeys.length > 0) {
      const role = pieceRoles[crazyKeys[crazyKeys.length - 1] - 1],
        color = ctrl.data.player.color,
        crazyData = ctrl.data.crazyhouse;
      if (!crazyData) return;

      const nb = crazyData.pockets[color === "white" ? 0 : 1][role];
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
  window.lishogi.pubsub.on("ply", () => {
    if (crazyKeys.length > 0) setDrop();
  });

  for (let i = 1; i <= 5; i++) {
    const iStr = i.toString();
    k.bind(iStr, (e: KeyboardEvent) => {
      e.preventDefault();
      if (!crazyKeys.includes(i)) {
        crazyKeys.push(i);
        setDrop();
      }
    });
    k.bind(
      iStr,
      (e: KeyboardEvent) => {
        e.preventDefault();
        const idx = crazyKeys.indexOf(i);
        if (idx >= 0) {
          crazyKeys.splice(idx, 1);
          if (idx === crazyKeys.length) {
            setDrop();
          }
        }
      },
      "keyup"
    );
  }

  const resetKeys = () => {
    if (crazyKeys.length > 0) {
      crazyKeys.length = 0;
      setDrop();
    }
  };

  window.addEventListener("blur", resetKeys);

  // Handle focus on input bars – these will hide keyup events
  window.addEventListener(
    "focus",
    (e) => {
      if (e.target && (e.target as HTMLElement).localName === "input")
        resetKeys();
    },
    { capture: true }
  );

  if (li.storage.get("crazyKeyHist") !== "0") preloadMouseIcons(ctrl.data);
}

// zh keys has unacceptable jank when cursors need to dl,
// so preload when the feature might be used.
// Images are used in _zh.scss, which should be kept in sync.
function preloadMouseIcons(data: RoundData) {
  const colorKey = data.player.color === "white" ? "w" : "b";
  if (window.fetch !== undefined) {
    for (const pKey of "PNBRSGL") {
      fetch(li.assetUrl(`piece/cburnett/${colorKey}${pKey}.svg`));
    }
  }
  mouseIconsLoaded = true;
}
