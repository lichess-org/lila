"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var util_1 = require("./util");
var util = require("./util");
var anim_1 = require("./anim");
function render(s) {
    var asWhite = s.orientation === 'white', posToTranslate = s.dom.relative ? util.posToTranslateRel : util.posToTranslateAbs(s.dom.bounds()), translate = s.dom.relative ? util.translateRel : util.translateAbs, boardEl = s.dom.elements.board, pieces = s.pieces, curAnim = s.animation.current, anims = curAnim ? curAnim.plan.anims : {}, fadings = curAnim ? curAnim.plan.fadings : {}, temporaryPieces = curAnim ? curAnim.plan.captures : {}, temporaryRoles = curAnim ? curAnim.plan.tempRole : {}, curDrag = s.draggable.current, squares = computeSquareClasses(s), samePieces = {}, sameSquares = {}, movedPieces = {}, movedSquares = {}, piecesKeys = Object.keys(pieces);
    var k, p, el, pieceAtKey, elPieceName, anim, fading, tempPiece, tempRole, pMvdset, pMvd, sMvdset, sMvd;
    el = boardEl.firstChild;
    while (el) {
        k = el.cgKey;
        if (isPieceNode(el)) {
            pieceAtKey = pieces[k];
            anim = anims[k];
            fading = fadings[k];
            tempPiece = temporaryPieces[k];
            tempRole = temporaryRoles[k];
            elPieceName = el.cgPiece;
            if (el.cgDragging && (!curDrag || curDrag.orig !== k)) {
                el.classList.remove('dragging');
                translate(el, posToTranslate(util_1.key2pos(k), asWhite, 0));
                el.cgDragging = false;
            }
            if (!fading && el.cgFading) {
                el.cgFading = false;
                el.classList.remove('fading');
            }
            if (el.classList.contains('temporary') && tempPiece) {
                var fullPieceName = pieceNameOf(tempPiece) + " temporary";
                if (elPieceName !== fullPieceName)
                    el.className = fullPieceName;
                samePieces[k] = true;
            }
            else if (pieceAtKey) {
                if (anim && el.cgAnimating && elPieceName === pieceNameOf(pieceAtKey)) {
                    var pos = util_1.key2pos(k);
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    if (curAnim && curAnim.plan.nextPlan && curAnim.plan.nextPlan.anims[k] && (!anim_1.isObjectEmpty(curAnim.plan.nextPlan.anims[k]) || !anim_1.isObjectEmpty(curAnim.plan.nextPlan.fadings[k]))) {
                        pos[0] += curAnim.plan.nextPlan.anims[k][2];
                        pos[1] += curAnim.plan.nextPlan.anims[k][3];
                    }
                    el.classList.add('anim');
                    if (tempRole) {
                        el.className = el.className.replace(pieceAtKey.role, tempRole);
                        el.classList.add('temprole');
                    }
                    else if (el.classList.contains('temprole')) {
                        el.classList.remove('temprole');
                        if (pieceAtKey.role === 'king')
                            el.className = el.className.replace('man', 'king');
                        else if (pieceAtKey.role === 'man')
                            el.className = el.className.replace('king', 'man');
                    }
                    translate(el, posToTranslate(pos, asWhite, anim[4]));
                }
                else if (el.cgAnimating) {
                    el.cgAnimating = false;
                    el.classList.remove('anim');
                    if (el.classList.contains('temprole')) {
                        el.classList.remove('temprole');
                        if (pieceAtKey.role === 'king')
                            el.className = el.className.replace('man', 'king');
                        else if (pieceAtKey.role === 'man')
                            el.className = el.className.replace('king', 'man');
                    }
                    translate(el, posToTranslate(util_1.key2pos(k), asWhite, 0));
                    if (s.addPieceZIndex)
                        el.style.zIndex = posZIndex(util_1.key2pos(k), asWhite);
                }
                if (elPieceName === pieceNameOf(pieceAtKey) && (!fading || !el.cgFading)) {
                    samePieces[k] = true;
                }
                else {
                    if (fading && elPieceName === pieceNameOf(fading)) {
                        el.classList.add('fading');
                        el.cgFading = true;
                    }
                    else {
                        if (movedPieces[elPieceName])
                            movedPieces[elPieceName].push(el);
                        else
                            movedPieces[elPieceName] = [el];
                    }
                }
            }
            else {
                if (movedPieces[elPieceName])
                    movedPieces[elPieceName].push(el);
                else
                    movedPieces[elPieceName] = [el];
            }
        }
        else if (isSquareNode(el)) {
            var cn = el.className;
            if (squares[k] === cn)
                sameSquares[k] = true;
            else if (movedSquares[cn])
                movedSquares[cn].push(el);
            else
                movedSquares[cn] = [el];
        }
        el = el.nextSibling;
    }
    for (var sk in squares) {
        if (!sameSquares[sk]) {
            sMvdset = movedSquares[squares[sk]];
            sMvd = sMvdset && sMvdset.pop();
            var translation = posToTranslate(util_1.key2pos(sk), asWhite, 0);
            if (sMvd) {
                sMvd.cgKey = sk;
                translate(sMvd, translation);
            }
            else {
                var squareNode = util_1.createEl('square', squares[sk]);
                squareNode.cgKey = sk;
                translate(squareNode, translation);
                boardEl.insertBefore(squareNode, boardEl.firstChild);
            }
        }
    }
    for (var j in piecesKeys) {
        k = piecesKeys[j];
        p = pieces[k];
        anim = anims[k];
        tempPiece = temporaryPieces[k];
        if (!samePieces[k] && !tempPiece) {
            pMvdset = movedPieces[pieceNameOf(p)];
            pMvd = pMvdset && pMvdset.pop();
            if (pMvd) {
                pMvd.cgKey = k;
                if (pMvd.cgFading) {
                    pMvd.classList.remove('fading');
                    pMvd.cgFading = false;
                }
                var pos = util_1.key2pos(k);
                if (s.addPieceZIndex)
                    pMvd.style.zIndex = posZIndex(pos, asWhite);
                var shift = void 0;
                if (anim) {
                    pMvd.cgAnimating = true;
                    pMvd.classList.add('anim');
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    shift = anim[4];
                }
                else
                    shift = 0;
                translate(pMvd, posToTranslate(pos, asWhite, shift));
            }
            else {
                var pieceName = pieceNameOf(p), pieceNode = util_1.createEl('piece', pieceName), pos = util_1.key2pos(k);
                pieceNode.cgPiece = pieceName;
                pieceNode.cgKey = k;
                var shift = void 0;
                if (anim) {
                    pieceNode.cgAnimating = true;
                    pos[0] += anim[2];
                    pos[1] += anim[3];
                    shift = anim[4];
                }
                else
                    shift = 0;
                translate(pieceNode, posToTranslate(pos, asWhite, shift));
                if (s.addPieceZIndex)
                    pieceNode.style.zIndex = posZIndex(pos, asWhite);
                boardEl.appendChild(pieceNode);
            }
        }
    }
    for (var i in temporaryPieces) {
        tempPiece = temporaryPieces[i];
        k = i;
        if (tempPiece && !samePieces[k]) {
            var pieceName = pieceNameOf(tempPiece) + " temporary", pieceNode = util_1.createEl('piece', pieceName), pos = util_1.key2pos(k);
            pieceNode.cgPiece = pieceName;
            pieceNode.cgKey = k;
            translate(pieceNode, posToTranslate(pos, asWhite, 0));
            if (s.addPieceZIndex)
                pieceNode.style.zIndex = posZIndex(pos, asWhite);
            boardEl.appendChild(pieceNode);
        }
    }
    for (var i in movedPieces)
        removeNodes(s, movedPieces[i]);
    for (var i in movedSquares)
        removeNodes(s, movedSquares[i]);
}
exports.default = render;
function isPieceNode(el) {
    return el.tagName === 'PIECE';
}
function isSquareNode(el) {
    return el.tagName === 'SQUARE';
}
function removeNodes(s, nodes) {
    for (var i in nodes)
        s.dom.elements.board.removeChild(nodes[i]);
}
function posZIndex(pos, asWhite) {
    var z = 2 + (pos[1] - 1) * 8 + (8 - pos[0]);
    if (asWhite)
        z = 67 - z;
    return z + '';
}
function pieceNameOf(piece) {
    if (piece.role === 'ghostman')
        return piece.color + " man ghost";
    else if (piece.role === 'ghostking')
        return piece.color + " king ghost";
    else
        return piece.color + " " + piece.role;
}
exports.pieceNameOf = pieceNameOf;
function computeSquareClasses(s) {
    var squares = {};
    var i, k;
    if (s.lastMove && s.highlight.lastMove)
        for (i in s.lastMove) {
            if (s.lastMove[i] !== s.selected)
                addSquare(squares, s.lastMove[i], 'last-move');
        }
    if (s.selected) {
        addSquare(squares, s.selected, 'selected');
        if (s.movable.showDests) {
            var dests = s.movable.dests && s.movable.dests[s.selected];
            if (dests)
                for (i in dests) {
                    k = dests[i];
                    addSquare(squares, k, 'move-dest' + (s.pieces[k] ? ' oc' : ''));
                }
            var pDests = s.premovable.dests;
            if (pDests)
                for (i in pDests) {
                    k = pDests[i];
                    addSquare(squares, k, 'premove-dest' + (s.pieces[k] ? ' oc' : ''));
                }
        }
    }
    var premove = s.premovable.current;
    if (premove)
        for (i in premove)
            addSquare(squares, premove[i], 'current-premove');
    else if (s.predroppable.current)
        addSquare(squares, s.predroppable.current.key, 'current-premove');
    var o = s.exploding;
    if (o)
        for (i in o.keys)
            addSquare(squares, o.keys[i], 'exploding' + o.stage);
    return squares;
}
function addSquare(squares, key, klass) {
    if (squares[key])
        squares[key] += ' ' + klass;
    else
        squares[key] = klass;
}
