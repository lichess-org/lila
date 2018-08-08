"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var util_1 = require("./util");
var premove_1 = require("./premove");
function callUserFunction(f) {
    var args = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        args[_i - 1] = arguments[_i];
    }
    if (f)
        setTimeout(function () { return f.apply(void 0, args); }, 1);
}
exports.callUserFunction = callUserFunction;
function toggleOrientation(state) {
    state.orientation = util_1.opposite(state.orientation);
    state.animation.current =
        state.draggable.current =
            state.selected = undefined;
}
exports.toggleOrientation = toggleOrientation;
function reset(state) {
    state.lastMove = undefined;
    unselect(state);
    unsetPremove(state);
    unsetPredrop(state);
}
exports.reset = reset;
function setPieces(state, pieces) {
    for (var key in pieces) {
        var piece = pieces[key];
        if (piece)
            state.pieces[key] = piece;
        else
            delete state.pieces[key];
    }
}
exports.setPieces = setPieces;
function setPremove(state, orig, dest, meta) {
    unsetPredrop(state);
    state.premovable.current = [orig, dest];
    callUserFunction(state.premovable.events.set, orig, dest, meta);
}
function unsetPremove(state) {
    if (state.premovable.current) {
        state.premovable.current = undefined;
        callUserFunction(state.premovable.events.unset);
    }
}
exports.unsetPremove = unsetPremove;
function setPredrop(state, role, key) {
    unsetPremove(state);
    state.predroppable.current = {
        role: role,
        key: key
    };
    callUserFunction(state.predroppable.events.set, role, key);
}
function unsetPredrop(state) {
    var pd = state.predroppable;
    if (pd.current) {
        pd.current = undefined;
        callUserFunction(pd.events.unset);
    }
}
exports.unsetPredrop = unsetPredrop;
function calcCaptKey(pieces, startX, startY, destX, destY) {
    var xDiff = destX - startX, yDiff = destY - startY;
    var yStep = yDiff === 0 ? 0 : (yDiff > 0 ? ((xDiff === 0 && Math.abs(yDiff) >= 2) ? 2 : 1) : ((xDiff === 0 && Math.abs(yDiff) >= 2) ? -2 : -1));
    var xStep = xDiff === 0 ? 0 : (yDiff === 0 ? (xDiff > 0 ? 1 : -1) : (startY % 2 == 0 ? (xDiff < 0 ? -1 : 0) : (xDiff > 0 ? 1 : 0)));
    if (xStep === 0 && yStep === 0)
        return undefined;
    var captPos = [startX + xStep, startY + yStep];
    if (captPos === undefined)
        return undefined;
    var captKey = util_1.pos2key(captPos);
    var piece = pieces[captKey];
    if (piece !== undefined && piece.role !== 'ghostman' && piece.role !== 'ghostking')
        return captKey;
    else
        return calcCaptKey(pieces, startX + xStep, startY + yStep, destX, destY);
}
exports.calcCaptKey = calcCaptKey;
function baseMove(state, orig, dest) {
    if (orig === dest || !state.pieces[orig])
        return false;
    var origPos = util_1.key2pos(orig), destPos = util_1.key2pos(dest);
    var isCapture = (state.movable.captLen !== undefined && state.movable.captLen > 0);
    var captKey = isCapture ? calcCaptKey(state.pieces, origPos[0], origPos[1], destPos[0], destPos[1]) : undefined;
    var captPiece = (isCapture && captKey) ? state.pieces[captKey] : undefined;
    var origPiece = state.pieces[orig];
    if (dest == state.selected)
        unselect(state);
    callUserFunction(state.events.move, orig, dest, captPiece);
    if (!state.movable.free && (state.movable.captLen === undefined || state.movable.captLen <= 1) && origPiece.role === 'man' && ((origPiece.color === 'white' && destPos[1] === 1) || (origPiece.color === 'black' && destPos[1] === 10)))
        state.pieces[dest] = {
            role: 'king',
            color: origPiece.color
        };
    else
        state.pieces[dest] = state.pieces[orig];
    delete state.pieces[orig];
    if (isCapture && captKey) {
        var captColor = state.pieces[captKey].color;
        var captRole = state.pieces[captKey].role;
        delete state.pieces[captKey];
        if (state.movable.captLen !== undefined && state.movable.captLen > 1) {
            if (captRole === 'man') {
                state.pieces[captKey] = {
                    role: 'ghostman',
                    color: captColor
                };
            }
            else if (captRole === 'king') {
                state.pieces[captKey] = {
                    role: 'ghostking',
                    color: captColor
                };
            }
        }
        else {
            for (var i = 0; i < util_1.allKeys.length; i++) {
                var pc = state.pieces[util_1.allKeys[i]];
                if (pc !== undefined && (pc.role == 'ghostking' || pc.role == 'ghostman'))
                    delete state.pieces[util_1.allKeys[i]];
            }
        }
    }
    if (state.lastMove && state.lastMove.length > 0 && isCapture) {
        if (state.lastMove[state.lastMove.length - 1] == orig)
            state.lastMove.push(dest);
        else
            state.lastMove = [orig, dest];
    }
    else
        state.lastMove = [orig, dest];
    callUserFunction(state.events.change);
    return captPiece || true;
}
exports.baseMove = baseMove;
function baseNewPiece(state, piece, key, force) {
    if (state.pieces[key]) {
        if (force)
            delete state.pieces[key];
        else
            return false;
    }
    callUserFunction(state.events.dropNewPiece, piece, key);
    state.pieces[key] = piece;
    state.lastMove = [key];
    callUserFunction(state.events.change);
    state.movable.dests = undefined;
    state.turnColor = util_1.opposite(state.turnColor);
    return true;
}
exports.baseNewPiece = baseNewPiece;
function baseUserMove(state, orig, dest) {
    var result = baseMove(state, orig, dest);
    if (result) {
        state.movable.dests = undefined;
        if (!state.movable.captLen || state.movable.captLen <= 1)
            state.turnColor = util_1.opposite(state.turnColor);
        state.animation.current = undefined;
    }
    return result;
}
function userMove(state, orig, dest) {
    if (canMove(state, orig, dest)) {
        var result = baseUserMove(state, orig, dest);
        if (result) {
            var holdTime = state.hold.stop();
            unselect(state);
            var metadata = {
                premove: false,
                ctrlKey: state.stats.ctrlKey,
                holdTime: holdTime
            };
            if (result !== true)
                metadata.captured = result;
            callUserFunction(state.movable.events.after, orig, dest, metadata);
            return true;
        }
    }
    else if (canPremove(state, orig, dest)) {
        setPremove(state, orig, dest, {
            ctrlKey: state.stats.ctrlKey
        });
        unselect(state);
    }
    else if (isMovable(state, dest) || isPremovable(state, dest)) {
        setSelected(state, dest);
        state.hold.start();
    }
    else
        unselect(state);
    return false;
}
exports.userMove = userMove;
function dropNewPiece(state, orig, dest, force) {
    if (canDrop(state, orig, dest) || force) {
        var piece = state.pieces[orig];
        delete state.pieces[orig];
        baseNewPiece(state, piece, dest, force);
        callUserFunction(state.movable.events.afterNewPiece, piece.role, dest, {
            predrop: false
        });
    }
    else if (canPredrop(state, orig, dest)) {
        setPredrop(state, state.pieces[orig].role, dest);
    }
    else {
        unsetPremove(state);
        unsetPredrop(state);
    }
    delete state.pieces[orig];
    unselect(state);
}
exports.dropNewPiece = dropNewPiece;
function selectSquare(state, key, force) {
    if (state.selected) {
        if (state.selected === key && !state.draggable.enabled) {
            unselect(state);
            state.hold.cancel();
        }
        else if ((state.selectable.enabled || force) && state.selected !== key) {
            if (userMove(state, state.selected, key)) {
                state.stats.dragged = false;
                if (state.movable.captLen !== undefined && state.movable.captLen > 1)
                    setSelected(state, key);
            }
        }
        else
            state.hold.start();
    }
    else if (isMovable(state, key) || isPremovable(state, key)) {
        setSelected(state, key);
        state.hold.start();
    }
    callUserFunction(state.events.select, key);
}
exports.selectSquare = selectSquare;
function setSelected(state, key) {
    state.selected = key;
    if (isPremovable(state, key)) {
        state.premovable.dests = premove_1.default(state.pieces, key, state.premovable.variant);
    }
    else
        state.premovable.dests = undefined;
}
exports.setSelected = setSelected;
function unselect(state) {
    state.selected = undefined;
    state.premovable.dests = undefined;
    state.hold.cancel();
}
exports.unselect = unselect;
function isMovable(state, orig) {
    var piece = state.pieces[orig];
    return piece && (state.movable.color === 'both' || (state.movable.color === piece.color &&
        state.turnColor === piece.color));
}
function canMove(state, orig, dest) {
    return orig !== dest && isMovable(state, orig) && (state.movable.free || (!!state.movable.dests && util_1.containsX(state.movable.dests[orig], dest)));
}
exports.canMove = canMove;
function canDrop(state, orig, dest) {
    var piece = state.pieces[orig];
    return piece && dest && (orig === dest || !state.pieces[dest]) && (state.movable.color === 'both' || (state.movable.color === piece.color &&
        state.turnColor === piece.color));
}
function isPremovable(state, orig) {
    var piece = state.pieces[orig];
    return piece && state.premovable.enabled &&
        state.movable.color === piece.color &&
        state.turnColor !== piece.color;
}
function canPremove(state, orig, dest) {
    return orig !== dest &&
        isPremovable(state, orig) &&
        util_1.containsX(premove_1.default(state.pieces, orig, state.premovable.variant), dest);
}
function canPredrop(state, orig, dest) {
    var piece = state.pieces[orig];
    return piece && dest &&
        (!state.pieces[dest] || state.pieces[dest].color !== state.movable.color) &&
        state.predroppable.enabled &&
        state.movable.color === piece.color &&
        state.turnColor !== piece.color;
}
function isDraggable(state, orig) {
    var piece = state.pieces[orig];
    return piece && state.draggable.enabled && (state.movable.color === 'both' || (state.movable.color === piece.color && (state.turnColor === piece.color || state.premovable.enabled)));
}
exports.isDraggable = isDraggable;
function playPremove(state) {
    var move = state.premovable.current;
    if (!move)
        return false;
    var orig = move[0], dest = move[1];
    var success = false;
    if (canMove(state, orig, dest)) {
        var result = baseUserMove(state, orig, dest);
        if (result) {
            var metadata = { premove: true };
            if (result !== true)
                metadata.captured = result;
            callUserFunction(state.movable.events.after, orig, dest, metadata);
            success = true;
        }
    }
    unsetPremove(state);
    return success;
}
exports.playPremove = playPremove;
function playPredrop(state, validate) {
    var drop = state.predroppable.current, success = false;
    if (!drop)
        return false;
    if (validate(drop)) {
        var piece = {
            role: drop.role,
            color: state.movable.color
        };
        if (baseNewPiece(state, piece, drop.key)) {
            callUserFunction(state.movable.events.afterNewPiece, drop.role, drop.key, {
                predrop: true
            });
            success = true;
        }
    }
    unsetPredrop(state);
    return success;
}
exports.playPredrop = playPredrop;
function cancelMove(state) {
    unsetPremove(state);
    unsetPredrop(state);
    unselect(state);
}
exports.cancelMove = cancelMove;
function stop(state) {
    state.movable.color =
        state.movable.dests =
            state.animation.current = undefined;
    cancelMove(state);
}
exports.stop = stop;
function getKeyAtDomPos(pos, asWhite, bounds) {
    var row = Math.ceil(10 * ((pos[1] - bounds.top) / bounds.height));
    if (!asWhite)
        row = 11 - row;
    var col = Math.ceil(10 * ((pos[0] - bounds.left) / bounds.width));
    if (!asWhite)
        col = 11 - col;
    if (row % 2 !== 0) {
        if (col % 2 !== 0)
            return undefined;
        else
            col = col / 2;
    }
    else {
        if (col % 2 === 0)
            return undefined;
        else
            col = (col + 1) / 2;
    }
    return (col > 0 && col < 6 && row > 0 && row < 11) ? util_1.pos2key([col, row]) : undefined;
}
exports.getKeyAtDomPos = getKeyAtDomPos;
