import { State } from './state'
import { key2pos, createEl } from './util'
import * as util from './util'
import { AnimCurrent, AnimVectors, AnimVector, AnimFadings, AnimCaptures, AnimRoles, isObjectEmpty } from './anim'
import { DragCurrent } from './drag'
import * as cg from './types'

// `$color $role`
type PieceName = string;

interface SamePieces { [key: string]: boolean }
interface SameSquares { [key: string]: boolean }
interface MovedPieces { [pieceName: string]: cg.PieceNode[] }
interface MovedSquares { [className: string]: cg.SquareNode[] }
interface SquareClasses { [key: string]: string }

// ported from https://github.com/veloce/lichobile/blob/master/src/js/draughtsground/view.js
// in case of bugs, blame @veloce
export default function render(s: State): void {
    const asWhite: boolean = s.orientation === 'white',
        posToTranslate = s.dom.relative ? util.posToTranslateRel : util.posToTranslateAbs(s.dom.bounds()),
        translate = s.dom.relative ? util.translateRel : util.translateAbs,
        boardEl: HTMLElement = s.dom.elements.board,
        pieces: cg.Pieces = s.pieces,
        curAnim: AnimCurrent | undefined = s.animation.current,
        anims: AnimVectors = curAnim ? curAnim.plan.anims : {},
        fadings: AnimFadings = curAnim ? curAnim.plan.fadings : {},
        temporaryPieces: AnimCaptures = curAnim ? curAnim.plan.captures : {},
        temporaryRoles: AnimRoles = curAnim ? curAnim.plan.tempRole : {},
        curDrag: DragCurrent | undefined = s.draggable.current,
        squares: SquareClasses = computeSquareClasses(s),
        samePieces: SamePieces = {},
        sameSquares: SameSquares = {},
        movedPieces: MovedPieces = {},
        movedSquares: MovedSquares = {},
        piecesKeys: cg.Key[] = Object.keys(pieces) as cg.Key[];
    let k: cg.Key,
        p: cg.Piece | undefined,
        el: cg.PieceNode | cg.SquareNode,
        pieceAtKey: cg.Piece | undefined,
        elPieceName: PieceName,
        anim: AnimVector | undefined,
        fading: cg.Piece | undefined,
        tempPiece: cg.Piece | undefined,
        tempRole: cg.Role | undefined,
        pMvdset: cg.PieceNode[],
        pMvd: cg.PieceNode | undefined,
        sMvdset: cg.SquareNode[],
        sMvd: cg.SquareNode | undefined;

    // walk over all board dom elements, apply animations and flag moved pieces
    el = boardEl.firstChild as cg.PieceNode | cg.SquareNode;
    while (el) {
        k = el.cgKey;
        if (isPieceNode(el)) {
            pieceAtKey = pieces[k];
            anim = anims[k];
            fading = fadings[k];
            tempPiece = temporaryPieces[k];
            tempRole = temporaryRoles[k];
            elPieceName = el.cgPiece;
            // if piece not being dragged anymore, remove dragging style
            if (el.cgDragging && (!curDrag || curDrag.orig !== k)) {
                el.classList.remove('dragging');
                translate(el, posToTranslate(key2pos(k), asWhite, 0));
                el.cgDragging = false;
            }
            // remove fading class if it still remains
            if (!fading && el.cgFading) {
                el.cgFading = false;
                el.classList.remove('fading');
            }

            if (el.classList.contains('temporary') && tempPiece ) {
                // Piece belongs here, check if it still has the right properties
                const fullPieceName = pieceNameOf(tempPiece) + " temporary";
                if (elPieceName !== fullPieceName)
                    el.className = fullPieceName;    
                samePieces[k] = true;
            } else if (pieceAtKey) {

                // there is now a piece at this dom key

                // continue animation if already animating and same piece
                // (otherwise it could animate a captured piece)
                if (anim && el.cgAnimating && elPieceName === pieceNameOf(pieceAtKey)) {
                    const pos = key2pos(k);
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    if (curAnim && curAnim.plan.nextPlan && curAnim.plan.nextPlan.anims[k] && (!isObjectEmpty(curAnim.plan.nextPlan.anims[k]) || !isObjectEmpty(curAnim.plan.nextPlan.fadings[k]))) {
                        pos[0] += curAnim.plan.nextPlan.anims[k][2];
                        pos[1] += curAnim.plan.nextPlan.anims[k][3];
                    }
                    el.classList.add('anim');
                    if (tempRole) {
                        el.className = el.className.replace(pieceAtKey.role, tempRole);
                        el.classList.add('temprole');
                    } else if (el.classList.contains('temprole')) {
                        el.classList.remove('temprole');
                        if (pieceAtKey.role === 'king')
                            el.className = el.className.replace('man', 'king');
                        else if (pieceAtKey.role === 'man')
                            el.className = el.className.replace('king', 'man');
                    }
                    translate(el, posToTranslate(pos, asWhite, anim[4]));
                } else if (el.cgAnimating) {
                    el.cgAnimating = false;
                    el.classList.remove('anim');
                    if (el.classList.contains('temprole')) {
                        el.classList.remove('temprole');
                        if (pieceAtKey.role === 'king')
                            el.className = el.className.replace('man', 'king');
                        else if (pieceAtKey.role === 'man')
                            el.className = el.className.replace('king', 'man');
                    }
                    translate(el, posToTranslate(key2pos(k), asWhite, 0));
                    if (s.addPieceZIndex) el.style.zIndex = posZIndex(key2pos(k), asWhite);
                }

                // same piece: flag as same
                if (elPieceName === pieceNameOf(pieceAtKey) && (!fading || !el.cgFading)) {
                    samePieces[k] = true;
                }
                // different piece: flag as moved unless it is a fading piece
                else {
                    if (fading && elPieceName === pieceNameOf(fading)) {
                        el.classList.add('fading');
                        el.cgFading = true;
                    } else {
                        if (movedPieces[elPieceName]) movedPieces[elPieceName].push(el);
                        else movedPieces[elPieceName] = [el];
                    }
                }

            } else {
                // no piece: flag as moved
                if (movedPieces[elPieceName]) movedPieces[elPieceName].push(el);
                else movedPieces[elPieceName] = [el];
            }
        }
        else if (isSquareNode(el)) {
            const cn = el.className;
            if (squares[k] === cn) sameSquares[k] = true;
            else if (movedSquares[cn]) movedSquares[cn].push(el);
            else movedSquares[cn] = [el];
        }
        el = el.nextSibling as cg.PieceNode | cg.SquareNode;
    }

    // walk over all squares in current set, apply dom changes to moved squares
    // or append new squares
    for (const sk in squares) {
        if (!sameSquares[sk]) {
            sMvdset = movedSquares[squares[sk]];
            sMvd = sMvdset && sMvdset.pop();
            const translation = posToTranslate(key2pos(sk as cg.Key), asWhite, 0);
            if (sMvd) {
                sMvd.cgKey = sk as cg.Key;
                translate(sMvd, translation);
            }
            else {
                const squareNode = createEl('square', squares[sk]) as cg.SquareNode;
                squareNode.cgKey = sk as cg.Key;
                translate(squareNode, translation);
                boardEl.insertBefore(squareNode, boardEl.firstChild);
            }
        }
    }

    // walk over all pieces in current set, apply dom changes to moved pieces
    // or append new pieces
    for (const j in piecesKeys) {
        k = piecesKeys[j];
        p = pieces[k];
        anim = anims[k];
        tempPiece = temporaryPieces[k];
        if (!samePieces[k] && !tempPiece) {
            pMvdset = movedPieces[pieceNameOf(p)];
            pMvd = pMvdset && pMvdset.pop();
            // a same piece was moved
            if (pMvd) {
                // apply dom changes
                pMvd.cgKey = k;
                if (pMvd.cgFading) {
                    pMvd.classList.remove('fading');
                    pMvd.cgFading = false;
                }
                const pos = key2pos(k);
                if (s.addPieceZIndex) pMvd.style.zIndex = posZIndex(pos, asWhite);
                let shift: number;
                if (anim) {
                    pMvd.cgAnimating = true;
                    pMvd.classList.add('anim');
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    shift = anim[4];
                } else shift = 0
                translate(pMvd, posToTranslate(pos, asWhite, shift));
            }
            // no piece in moved obj: insert the new piece
            // new: assume the new piece is not being dragged
            // might be a bad idea
            else {

                const pieceName = pieceNameOf(p),
                    pieceNode = createEl('piece', pieceName) as cg.PieceNode,
                    pos = key2pos(k);

                pieceNode.cgPiece = pieceName;
                pieceNode.cgKey = k;
                let shift: number;
                if (anim) {
                    pieceNode.cgAnimating = true;
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    shift = anim[4];
                } else shift = 0
                translate(pieceNode, posToTranslate(pos, asWhite, shift));

                if (s.addPieceZIndex) pieceNode.style.zIndex = posZIndex(pos, asWhite);

                boardEl.appendChild(pieceNode);
            }
        }
    }

    for (const i in temporaryPieces) {
        tempPiece = temporaryPieces[i];
        k = i as cg.Key;
        if (tempPiece && !samePieces[k]) {
            const pieceName = pieceNameOf(tempPiece) + " temporary",
                pieceNode = createEl('piece', pieceName) as cg.PieceNode,
                pos = key2pos(k);
            pieceNode.cgPiece = pieceName;
            pieceNode.cgKey = k;
            translate(pieceNode, posToTranslate(pos, asWhite, 0));
            if (s.addPieceZIndex) pieceNode.style.zIndex = posZIndex(pos, asWhite);
            boardEl.appendChild(pieceNode);
        }
    }

    // remove any element that remains in the moved sets
    for (const i in movedPieces) removeNodes(s, movedPieces[i]);
    for (const i in movedSquares) removeNodes(s, movedSquares[i]);

}

function isPieceNode(el: cg.PieceNode | cg.SquareNode): el is cg.PieceNode {
  return el.tagName === 'PIECE';
}
function isSquareNode(el: cg.PieceNode | cg.SquareNode): el is cg.SquareNode {
  return el.tagName === 'SQUARE';
}

function removeNodes(s: State, nodes: HTMLElement[]): void {
  for (const i in nodes) s.dom.elements.board.removeChild(nodes[i]);
}

function posZIndex(pos: cg.Pos, asWhite: boolean): string {
  let z = 2 + (pos[1] - 1) * 8 + (8 - pos[0]);
  if (asWhite) z = 67 - z;
  return z + '';
}

export function pieceNameOf(piece: cg.Piece): string {
    if (piece.role === 'ghostman')
        return `${piece.color} man ghost`;
    else if (piece.role === 'ghostking')
        return `${piece.color} king ghost`;
    else
        return `${piece.color} ${piece.role}`;
}

function computeSquareClasses(s: State): SquareClasses {
  const squares: SquareClasses = {};
  let i: any, k: cg.Key;
  if (s.lastMove && s.highlight.lastMove) for (i in s.lastMove) {
    if (s.lastMove[i] !== s.selected)
        addSquare(squares, s.lastMove[i], 'last-move');
  }
  if (s.selected) {
    addSquare(squares, s.selected, 'selected');
    if (s.movable.showDests) {
      const dests = s.movable.dests && s.movable.dests[s.selected];
      if (dests) for (i in dests) {
        k = dests[i];
        addSquare(squares, k, 'move-dest' + (s.pieces[k] ? ' oc' : ''));
      }
      const pDests = s.premovable.dests;
      if (pDests) for (i in pDests) {
        k = pDests[i];
        addSquare(squares, k, 'premove-dest' + (s.pieces[k] ? ' oc' : ''));
      }
    }
  }
  const premove = s.premovable.current;
  if (premove) for (i in premove) addSquare(squares, premove[i], 'current-premove');
  else if (s.predroppable.current) addSquare(squares, s.predroppable.current.key, 'current-premove');

  const o = s.exploding;
  if (o) for (i in o.keys) addSquare(squares, o.keys[i], 'exploding' + o.stage);

  return squares;
}

function addSquare(squares: SquareClasses, key: cg.Key, klass: string): void {
  if (squares[key]) squares[key] += ' ' + klass;
  else squares[key] = klass;
}
