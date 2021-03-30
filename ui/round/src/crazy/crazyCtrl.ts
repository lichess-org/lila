import { isPlayerTurn } from "game/game";
import { dragNewPiece } from "shogiground/drag";
import { setDropMode, cancelDropMode } from "shogiground/drop";
import RoundController from "../ctrl";
import * as cg from "shogiground/types";
//import { RoundData } from "../interfaces";
import { Shogi } from "shogiops/shogi"; 
import { parseFen } from "shogiops/fen";
import { makeShogiFen, parseChessSquare } from "shogiops/compat";
import { PocketRole } from 'shogiops/types'

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

export function shadowDrop(ctrl: RoundController, color: Color, e: cg.MouchEvent): void {
  const el = e.target as HTMLElement;
  const role = (el.getAttribute("data-role") ??
    el.firstElementChild!.getAttribute("data-role")) as cg.Role;
  if (!ctrl.shogiground) return;
  const curPiece = ctrl.shogiground.state.drawable.piece;
  if (curPiece && curPiece.role == role && curPiece.color == color)
    ctrl.shogiground.state.drawable.piece = undefined
  else ctrl.shogiground.state.drawable.piece = { role: role, color: color };
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

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

export function selectToDrop(ctrl: RoundController, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !ctrl.isPlaying()) return;
  const el = e.target as HTMLElement,
    role = el.getAttribute("data-role") as cg.Role,
    color = el.getAttribute("data-color") as cg.Color,
    number = el.getAttribute("data-nb");
  if (!role || !color || number === "0") return;
  const dropMode = ctrl.shogiground?.state.dropmode;
  const dropPiece = ctrl.shogiground?.state.dropmode.piece;
  if(!dropMode.active || dropPiece?.role !== role){
    setDropMode(ctrl.shogiground.state, { color, role });
    ctrl.selectedPiece = {color, role};
  }
  else{
    cancelDropMode(ctrl.shogiground.state);
  }
  e.stopPropagation();
  e.preventDefault();
  ctrl.redraw();
}

let dropWithKey = false;
let dropWithDrag = false;

export function valid(
  ctrl: RoundController,
  role: cg.Role,
  key: cg.Key,
  checkmateCheck = false,
): boolean {
  const data = ctrl.data;
  const lastStep = data.steps[data.steps.length - 1];
  const move = {role: role as PocketRole, to: parseChessSquare(key)!};
  const color = ctrl.ply % 2 === 0 ? "white" : "black";

  // Unless reload event occurs we have only board fen, so we have to fake the rest
  const fen = lastStep.fen.split(' ').length > 1 ? lastStep.fen : lastStep.fen + " " + color[0] + ' rbgsnlpRBGSNLP';

  if (crazyKeys.length === 0) dropWithDrag = true;
  else
    dropWithKey = true;

  if (!isPlayerTurn(data)) return false;

  const shogi = Shogi.fromSetup(parseFen(makeShogiFen(fen)).unwrap(), false);
  const l = shogi.unwrap(
    (s) => s.isLegal(move!),
    _ => true // for weird positions
  );
  if(checkmateCheck && role === 'pawn' && shogi.isOk){
    shogi.unwrap().play(move!);
    if(shogi.unwrap().isCheckmate()) alert("Checkmating with a pawn drop is an illegal move.");
  }
  return l;
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
}
