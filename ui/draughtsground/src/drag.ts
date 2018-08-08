import { State } from './state'
import * as board from './board'
import * as util from './util'
import { clear as drawClear } from './draw'
import * as cg from './types'
import { anim } from './anim'
import { pieceNameOf } from './render'

export interface DragCurrent {
  /** orig key of dragging piece */
  orig: cg.Key;
  /** orig position of dragging piece */
  origPos: cg.Pos;
  piece: cg.Piece;
  /**  dom x; y of the piece at original position */
  rel: cg.NumberPair;
  /**  initial event dom position*/
  epos: cg.NumberPair; // 
  /** relative current dom position, or amount dragged compared to epos */
  pos: cg.NumberPair;
  dec: cg.NumberPair; // piece center decay
  /** whether the drag has started; as per the distance setting */
  started: boolean;
  element: cg.PieceNode | (() => cg.PieceNode | undefined);
  /** it it a new piece from outside the board */
  newPiece?: boolean;
  /** can the new piece replace an existing one (editor) */
  force?: boolean;
  previouslySelected?: cg.Key;
  originTarget: EventTarget;
}

export function start(s: State, e: cg.MouchEvent): void {
    if (e.button !== undefined && e.button !== 0) return; // only touch or left click
    if (e.touches && e.touches.length > 1) return; // support one finger touch only
    e.preventDefault();

    const asWhite = s.orientation === 'white',
        bounds = s.dom.bounds(),
        position = util.eventPosition(e) as cg.NumberPair,
        orig = board.getKeyAtDomPos(position, asWhite, bounds);

    if (!orig) return;

    const piece = s.pieces[orig];
    const previouslySelected = s.selected;
    if (!previouslySelected && s.drawable.enabled && (
        s.drawable.eraseOnClick || (!piece || piece.color !== s.turnColor)
    )) drawClear(s);
    const hadPremove = !!s.premovable.current;
    const hadPredrop = !!s.predroppable.current;
    s.stats.ctrlKey = e.ctrlKey;

    if (s.selected && board.canMove(s, s.selected, orig)) {
        anim(state => board.selectSquare(state, orig), s);
    } else {
        board.selectSquare(s, orig);
    }
    const stillSelected = s.selected === orig;
    const element = pieceElementByKey(s, orig);
    if (piece && element && stillSelected && board.isDraggable(s, orig)) {
        const squareBounds = computeSquareBounds(orig, asWhite, bounds);
        s.draggable.current = {
            orig: orig,
            origPos: util.key2pos(orig),
            piece: piece,
            rel: position,
            epos: position,
            pos: [0, 0],
            dec: s.draggable.centerPiece ? [
                position[0] - (squareBounds.left + squareBounds.width / 2),
                position[1] - (squareBounds.top + squareBounds.height / 2)
            ] : [0, 0],
            started: s.draggable.autoDistance && s.stats.dragged,
            element: element,
            previouslySelected: previouslySelected,
            originTarget: e.target
        };
        element.cgDragging = true;
        element.classList.add('dragging');
        // place ghost
        const ghost = s.dom.elements.ghost;
        if (ghost) {
            ghost.className = 'ghost ' + pieceNameOf(piece);
            util.translateAbs(ghost, util.posToTranslateAbs(bounds)(util.key2pos(orig), asWhite, 0));
        }
        processDrag(s);
    } else {
        if (hadPremove) board.unsetPremove(s);
        if (hadPredrop) board.unsetPredrop(s);
    }
    s.dom.redraw();
}

export function dragNewPiece(s: State, piece: cg.Piece, e: cg.MouchEvent, force?: boolean): void {

  const key: cg.Key = '00';

  s.pieces[key] = piece;

  s.dom.redraw();

  const position = util.eventPosition(e) as cg.NumberPair,
  asWhite = s.orientation === 'white',
  bounds = s.dom.bounds(),
  squareBounds = computeSquareBounds(key, asWhite, bounds);

  const rel: cg.NumberPair = [
    (asWhite ? -1 : 10) * squareBounds.width + bounds.left,
    (!asWhite ? 9 : 0) * squareBounds.height + bounds.top
  ];

  s.draggable.current = {
    orig: key,
    origPos: util.key2pos(key),
    piece: piece,
    rel: rel,
    epos: position,
    pos: [position[0] - rel[0], position[1] - rel[1]],
    dec: [-squareBounds.width / 2, -squareBounds.height / 2],
    started: true,
    element: () => pieceElementByKey(s, key),
    originTarget: e.target,
    newPiece: true,
    force: force || false
  };
  processDrag(s);
}

function processDrag(s: State): void {
  util.raf(() => {
    const cur = s.draggable.current;
    if (!cur) return;
    // cancel animations while dragging
    if (s.animation.current && s.animation.current.plan.anims[cur.orig]) s.animation.current = undefined;
    // if moving piece is gone, cancel
    const origPiece = s.pieces[cur.orig];
    if (!origPiece || !util.samePiece(origPiece, cur.piece)) cancel(s);
    else {
      if (!cur.started && util.distanceSq(cur.epos, cur.rel) >= Math.pow(s.draggable.distance, 2)) cur.started = true;
      if (cur.started) {

        // support lazy elements
        if (typeof cur.element === 'function') {
          const found = cur.element();
          if (!found) return;
          cur.element = found;
          cur.element.cgDragging = true;
          cur.element.classList.add('dragging');
        }

        const asWhite = s.orientation === 'white', bounds = s.dom.bounds();
        cur.pos = [
          cur.epos[0] - cur.rel[0],
          cur.epos[1] - cur.rel[1]
        ];

        // move piece
        const translation = util.posToTranslateAbs(bounds)(cur.origPos, asWhite, 0);
        translation[0] += cur.pos[0] + cur.dec[0];
        translation[1] += cur.pos[1] + cur.dec[1];
        util.translateAbs(cur.element, translation);

      }
    }
    processDrag(s);
  });
}

export function move(s: State, e: cg.MouchEvent): void {
  // support one finger touch only
  if (s.draggable.current && (!e.touches || e.touches.length < 2)) {
    s.draggable.current.epos = util.eventPosition(e) as cg.NumberPair;
  }
}

export function end(s: State, e: cg.MouchEvent): void {
    const cur = s.draggable.current;
    if (!cur) return;
    // comparing with the origin target is an easy way to test that the end event
    // has the same touch origin
    if (e.type === 'touchend' && cur && cur.originTarget !== e.target && !cur.newPiece) {
        s.draggable.current = undefined;
        return;
    }
    board.unsetPremove(s);
    board.unsetPredrop(s);
    // touchend has no position; so use the last touchmove position instead
    const eventPos: cg.NumberPair = util.eventPosition(e) || cur.epos;
    const dest = board.getKeyAtDomPos(eventPos, s.orientation === 'white', s.dom.bounds());
    if (dest && cur.started) {
        if (cur.newPiece) board.dropNewPiece(s, cur.orig, dest, cur.force);
        else {
            s.stats.ctrlKey = e.ctrlKey;
            //if (board.userMove(s, cur.orig, dest)) {
            //Add only fading animation here, as the dragged piece is dropped immediatly on the target square, but with draughts we never drop 
            //on top of the piece captured, so fading out should be equal with drag or point and click
            if (anim(state => board.userMove(state, cur.orig, dest), s, true)) {
                s.stats.dragged = true;
                //If we can continue capturing keep the piece selected to enable quickly clicking all target squares one after the other
                if (s.movable.captLen !== undefined && s.movable.captLen > 1)
                    board.setSelected(s, dest);
            }
        }
    } else if (cur.newPiece) {
        delete s.pieces[cur.orig];
    } else if (s.draggable.deleteOnDropOff) {
        delete s.pieces[cur.orig];
        board.callUserFunction(s.events.change);
    }
    if (cur && cur.orig === cur.previouslySelected && (cur.orig === dest || !dest))
        board.unselect(s);
    else if (!s.selectable.enabled) board.unselect(s);

    removeDragElements(s);

    s.draggable.current = undefined;
    s.dom.redraw();
}

export function cancel(s: State): void {
  const cur = s.draggable.current;
  if (cur) {
    if (cur.newPiece) delete s.pieces[cur.orig];
    s.draggable.current = undefined;
    board.unselect(s);
    removeDragElements(s);
    s.dom.redraw();
  }
}

function removeDragElements(s: State) {
  const e = s.dom.elements;
  if (e.ghost) util.translateAway(e.ghost);
}

function computeSquareBounds(key: cg.Key, asWhite: boolean, bounds: ClientRect) {

    const pos = util.key2pos(key);
    if (!asWhite) {
        pos[0] = 6 - pos[0];
        pos[1] = 11 - pos[1];
    }
    
    return {
        left: bounds.left + bounds.width * ((pos[0] - 1) * 2 + (pos[1] % 2 !== 0 ? 1 : 0)) / 10,
        top: bounds.top + bounds.height * (pos[1] - 1) / 10,
        width: bounds.width / 10,
        height: bounds.height / 10
    };

}

function pieceElementByKey(s: State, key: cg.Key): cg.PieceNode | undefined {
  let el = s.dom.elements.board.firstChild as cg.PieceNode;
  while (el) {
    if (el.cgKey === key && el.tagName === 'PIECE') return el;
    el = el.nextSibling as cg.PieceNode;
  }
  return undefined;
}
