import { State } from "./state";
import * as board from "./board";
import * as util from "./util";
import { clear as drawClear } from "./draw";
import * as cg from "./types";
import { anim } from "./anim";

export interface DragCurrent {
  orig: cg.Key; // orig key of dragging piece
  piece: cg.Piece;
  origPos: cg.NumberPair; // first event position
  pos: cg.NumberPair; // latest event position
  started: boolean; // whether the drag has started; as per the distance setting
  element: cg.PieceNode | (() => cg.PieceNode | undefined);
  newPiece?: boolean; // it it a new piece from outside the board
  force?: boolean; // can the new piece replace an existing one (editor)
  previouslySelected?: cg.Key;
  originTarget: EventTarget | null;
}

export function start(s: State, e: cg.MouchEvent): void {
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  if (e.touches && e.touches.length > 1) return; // support one finger touch only
  const bounds = s.dom.bounds(),
    position = util.eventPosition(e)!,
    orig = board.getKeyAtDomPos(position, board.whitePov(s), bounds);
  if (!orig) return;
  const piece = s.pieces.get(orig);
  const previouslySelected = s.selected;
  if (
    !previouslySelected &&
    s.drawable.enabled &&
    (s.drawable.eraseOnClick || !piece || piece.color !== s.turnColor)
  )
    drawClear(s);
  // Prevent touch scroll and create no corresponding mouse event, if there
  // is an intent to interact with the board. If no color is movable
  // (and the board is not for viewing only), touches are likely intended to
  // select squares.
  if (
    e.cancelable !== false &&
    (!e.touches ||
      !s.movable.color ||
      piece ||
      previouslySelected ||
      pieceCloseTo(s, position))
  )
    /* eslint-disable-line */
    e.preventDefault();
  const hadPremove = !!s.premovable.current;
  const hadPredrop = !!s.predroppable.current;
  s.stats.ctrlKey = e.ctrlKey;
  if (s.selected && board.canMove(s, s.selected, orig)) {
    anim((state) => board.selectSquare(state, orig), s);
  } else {
    board.selectSquare(s, orig);
  }
  const stillSelected = s.selected === orig;
  const element = pieceElementByKey(s, orig);
  if (piece && element && stillSelected && board.isDraggable(s, orig)) {
    s.draggable.current = {
      orig,
      piece,
      origPos: position,
      pos: position,
      started: s.draggable.autoDistance && s.stats.dragged,
      element,
      previouslySelected,
      originTarget: e.target,
    };
    element.cgDragging = true;
    element.classList.add("dragging");
    element.classList.remove("fix-blur");
    // place ghost
    const ghost = s.dom.elements.ghost;
    if (ghost) {
      ghost.className = `ghost ${piece.color} ${piece.role}`;
      util.translateAbs(
        ghost,
        util.posToTranslateAbs(bounds)(util.key2pos(orig), board.whitePov(s))
      );
      util.setVisible(ghost, true);
    }
    processDrag(s);
  } else {
    if (hadPremove) board.unsetPremove(s);
    if (hadPredrop) board.unsetPredrop(s);
  }
  s.dom.redraw();
}

function pieceCloseTo(s: State, pos: cg.NumberPair): boolean {
  const asWhite = board.whitePov(s),
    bounds = s.dom.bounds(),
    radiusSq = Math.pow(bounds.width / 9, 2);
  for (const key in s.pieces) {
    const center = computeSquareCenter(key as cg.Key, asWhite, bounds);
    if (util.distanceSq(center, pos) <= radiusSq) return true;
  }
  return false;
}

export function dragNewPiece(
  s: State,
  piece: cg.Piece,
  e: cg.MouchEvent,
  force?: boolean
): void {
  const key: cg.Key = "a0";
  s.pieces.set(key, piece);
  s.dom.redraw();

  const position = util.eventPosition(e)!;

  s.draggable.current = {
    orig: key,
    piece,
    origPos: position,
    pos: position,
    started: true,
    element: () => pieceElementByKey(s, key),
    originTarget: e.target,
    newPiece: true,
    force: !!force,
  };
  processDrag(s);
}

function processDrag(s: State): void {
  requestAnimationFrame(() => {
    const cur = s.draggable.current;
    if (!cur) return;
    // cancel animations while dragging
    if (s.animation.current?.plan.anims.has(cur.orig))
      s.animation.current = undefined;
    // if moving piece is gone, cancel
    const origPiece = s.pieces.get(cur.orig);
    if (!origPiece || !util.samePiece(origPiece, cur.piece)) cancel(s);
    else {
      if (
        !cur.started &&
        util.distanceSq(cur.pos, cur.origPos) >=
          Math.pow(s.draggable.distance, 2)
      )
        cur.started = true;
      if (cur.started) {
        // support lazy elements
        if (typeof cur.element === "function") {
          const found = cur.element();
          if (!found) return;
          found.cgDragging = true;
          found.classList.add("dragging");
          found.classList.remove("fix-blur");
          cur.element = found;
        }

        const bounds = s.dom.bounds();
        util.translateAbs(cur.element, [
          cur.pos[0] - bounds.left - bounds.width / 18,
          cur.pos[1] - bounds.top - bounds.height / 18,
        ]);
      }
    }
    processDrag(s);
  });
}

export function move(s: State, e: cg.MouchEvent): void {
  // support one finger touch only
  if (s.draggable.current && (!e.touches || e.touches.length < 2)) {
    /* eslint-disable-line */
    s.draggable.current.pos = util.eventPosition(e)!;
  }
}

export function end(s: State, e: cg.MouchEvent): void {
  const cur = s.draggable.current;
  if (!cur) return;
  // create no corresponding mouse event
  if (e.type === "touchend" && e.cancelable !== false) e.preventDefault();
  // comparing with the origin target is an easy way to test that the end event
  // has the same touch origin
  if (e.type === "touchend" && cur.originTarget !== e.target && !cur.newPiece) {
    s.draggable.current = undefined;
    return;
  }
  board.unsetPremove(s);
  board.unsetPredrop(s);
  // touchend has no position; so use the last touchmove position instead
  const eventPos = util.eventPosition(e) || cur.pos;
  const dest = board.getKeyAtDomPos(
    eventPos,
    board.whitePov(s),
    s.dom.bounds()
  );
  if (dest && cur.started && cur.orig !== dest) {
    if (cur.newPiece) board.dropNewPiece(s, cur.orig, dest, cur.force);
    else {
      s.stats.ctrlKey = e.ctrlKey;
      if (board.userMove(s, cur.orig, dest)) s.stats.dragged = true;
    }
  } else if (cur.newPiece) {
    s.pieces.delete(cur.orig);
  } else if (s.draggable.deleteOnDropOff && !dest) {
    s.pieces.delete(cur.orig);
    board.callUserFunction(s.events.change);
  }
  if (cur.orig === cur.previouslySelected && (cur.orig === dest || !dest))
    board.unselect(s);
  else if (!s.selectable.enabled) board.unselect(s);

  removeDragElements(s);

  s.draggable.current = undefined;
  s.dom.redraw();
}

export function cancel(s: State): void {
  const cur = s.draggable.current;
  if (cur) {
    if (cur.newPiece) s.pieces.delete(cur.orig);
    s.draggable.current = undefined;
    board.unselect(s);
    removeDragElements(s);
    s.dom.redraw();
  }
}

function removeDragElements(s: State): void {
  const e = s.dom.elements;
  if (e.ghost) util.setVisible(e.ghost, false);
}

function computeSquareCenter(
  key: cg.Key,
  asWhite: boolean,
  bounds: ClientRect
): cg.NumberPair {
  const pos = util.key2pos(key);
  if (!asWhite) {
    pos[0] = 8 - pos[0];
    pos[1] = 8 - pos[1];
  }
  return [
    bounds.left + (bounds.width * pos[0]) / 9 + bounds.width / 18,
    bounds.top + (bounds.height * (8 - pos[1])) / 9 + bounds.height / 18,
  ];
}

function pieceElementByKey(s: State, key: cg.Key): cg.PieceNode | undefined {
  let el = s.dom.elements.board.firstChild;
  while (el) {
    if (
      (el as cg.KeyedNode).cgKey === key &&
      (el as cg.KeyedNode).tagName === "PIECE"
    )
      return el as cg.PieceNode;
    el = el.nextSibling;
  }
  return;
}
