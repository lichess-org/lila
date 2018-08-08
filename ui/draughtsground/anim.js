"use strict";
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
var util = require("./util");
var board_1 = require("./board");
function anim(mutation, state, fadeOnly, noCaptSequences) {
    if (fadeOnly === void 0) { fadeOnly = false; }
    if (noCaptSequences === void 0) { noCaptSequences = false; }
    return state.animation.enabled ? animate(mutation, state, fadeOnly, noCaptSequences) : render(mutation, state);
}
exports.anim = anim;
function render(mutation, state) {
    var result = mutation(state);
    state.dom.redraw();
    return result;
}
exports.render = render;
function makePiece(key, piece) {
    return {
        key: key,
        pos: util.key2pos(key),
        piece: piece
    };
}
function closer(piece, pieces) {
    return pieces.sort(function (p1, p2) {
        return util.distanceSq(piece.pos, p1.pos) - util.distanceSq(piece.pos, p2.pos);
    })[0];
}
function ghostPiece(piece) {
    if (piece.role === 'man')
        return { role: 'ghostman', color: piece.color, promoted: piece.promoted };
    else if (piece.role === 'king')
        return { role: 'ghostking', color: piece.color, promoted: piece.promoted };
    else
        return { role: piece.role, color: piece.color, promoted: piece.promoted };
}
function isPromotable(p) {
    return (p.piece.color === 'white' && p.pos[1] === 1) || (p.piece.color === 'black' && p.pos[1] === 10);
}
function computePlan(prevPieces, current, fadeOnly, noCaptSequences) {
    if (fadeOnly === void 0) { fadeOnly = false; }
    if (noCaptSequences === void 0) { noCaptSequences = false; }
    var missingsW = [], missingsB = [], newsW = [], newsB = [];
    var prePieces = {}, samePieces = {};
    var curP, preP, i;
    for (i in prevPieces) {
        prePieces[i] = makePiece(i, prevPieces[i]);
    }
    for (var _i = 0, _a = util.allKeys; _i < _a.length; _i++) {
        var key = _a[_i];
        curP = current.pieces[key];
        preP = prePieces[key];
        if (curP) {
            if (preP) {
                if (!util.samePiece(curP, preP.piece)) {
                    if (preP.piece.color === 'white')
                        missingsW.push(preP);
                    else
                        missingsB.push(preP);
                    if (curP.color === 'white')
                        newsW.push(makePiece(key, curP));
                    else
                        newsB.push(makePiece(key, curP));
                }
            }
            else {
                if (curP.color === 'white')
                    newsW.push(makePiece(key, curP));
                else
                    newsB.push(makePiece(key, curP));
            }
        }
        else if (preP) {
            if (preP.piece.color === 'white')
                missingsW.push(preP);
            else
                missingsB.push(preP);
        }
    }
    var plan = { anims: {}, fadings: {}, captures: {}, tempRole: {} };
    var nextPlan = { anims: {}, fadings: {}, captures: {}, tempRole: {} };
    if (newsW.length > 1 && missingsW.length > 0) {
        newsW = newsW.sort(function (p1, p2) {
            return util.distanceSq(missingsW[0].pos, p1.pos) - util.distanceSq(missingsW[0].pos, p2.pos);
        });
    }
    if (newsB.length > 1 && missingsB.length > 0) {
        newsB = newsB.sort(function (p1, p2) {
            return util.distanceSq(missingsB[0].pos, p1.pos) - util.distanceSq(missingsB[0].pos, p2.pos);
        });
    }
    var missings = missingsW.concat(missingsB), news = newsW.concat(newsB);
    news.forEach(function (newP) {
        preP = closer(newP, missings.filter(function (p) {
            return !samePieces[p.key] &&
                newP.piece.color === p.piece.color &&
                (newP.piece.role === p.piece.role ||
                    (p.piece.role === 'man' && newP.piece.role === 'king' && isPromotable(newP)) ||
                    (p.piece.role === 'king' && newP.piece.role === 'man' && isPromotable(p)));
        }));
        if (preP && !fadeOnly) {
            samePieces[preP.key] = true;
            var tempRole = (preP.piece.role === 'man' && newP.piece.role === 'king' && isPromotable(newP)) ? 'man' : undefined;
            if (!noCaptSequences && current.lastMove && current.lastMove.length > 2 && current.lastMove[0] === preP.key && current.lastMove[current.lastMove.length - 1] === newP.key) {
                var lastPos = util.key2pos(current.lastMove[1]), newPos = void 0;
                plan.anims[newP.key] = getVector(preP.pos, lastPos);
                plan.nextPlan = nextPlan;
                if (tempRole)
                    plan.tempRole[newP.key] = tempRole;
                var captKeys_1 = new Array();
                var captKey = board_1.calcCaptKey(prevPieces, preP.pos[0], preP.pos[1], lastPos[0], lastPos[1]);
                if (captKey) {
                    captKeys_1.push(captKey);
                    prevPieces[captKey] = ghostPiece(prevPieces[captKey]);
                }
                plan.captures = {};
                missings.forEach(function (p) {
                    if (p.piece.color !== newP.piece.color) {
                        if (captKeys_1.indexOf(p.key) !== -1)
                            plan.captures[p.key] = ghostPiece(p.piece);
                        else
                            plan.captures[p.key] = p.piece;
                    }
                });
                var newPlan = { anims: {}, fadings: {}, captures: {}, tempRole: {} };
                for (i = 2; i < current.lastMove.length; i++) {
                    newPos = util.key2pos(current.lastMove[i]);
                    nextPlan.anims[newP.key] = getVector(lastPos, newPos);
                    nextPlan.anims[newP.key][2] = lastPos[0] - newP.pos[0];
                    nextPlan.anims[newP.key][3] = lastPos[1] - newP.pos[1];
                    nextPlan.nextPlan = newPlan;
                    if (tempRole)
                        nextPlan.tempRole[newP.key] = tempRole;
                    captKey = board_1.calcCaptKey(prevPieces, lastPos[0], lastPos[1], newPos[0], newPos[1]);
                    if (captKey) {
                        captKeys_1.push(captKey);
                        prevPieces[captKey] = ghostPiece(prevPieces[captKey]);
                    }
                    nextPlan.captures = {};
                    missings.forEach(function (p) {
                        if (p.piece.color !== newP.piece.color) {
                            if (captKeys_1.indexOf(p.key) !== -1)
                                nextPlan.captures[p.key] = ghostPiece(p.piece);
                            else
                                nextPlan.captures[p.key] = p.piece;
                        }
                    });
                    lastPos = newPos;
                    nextPlan = newPlan;
                    newPlan = { anims: {}, fadings: {}, captures: {}, tempRole: {} };
                }
            }
            else {
                plan.anims[newP.key] = getVector(preP.pos, newP.pos);
                if (tempRole)
                    plan.tempRole[newP.key] = tempRole;
            }
        }
    });
    return plan;
}
function getVector(preP, newP) {
    if (preP[1] % 2 === 0 && newP[1] % 2 === 0)
        return [preP[0] - newP[0], preP[1] - newP[1], 0, 0, -0.5];
    else if (preP[1] % 2 !== 0 && newP[1] % 2 === 0)
        return [preP[0] - newP[0] + 0.5, preP[1] - newP[1], 0, 0, -0.5];
    else if (preP[1] % 2 === 0 && newP[1] % 2 !== 0)
        return [preP[0] - newP[0] - 0.5, preP[1] - newP[1], 0, 0, 0];
    else
        return [preP[0] - newP[0], preP[1] - newP[1], 0, 0, 0];
}
var perf = window.performance !== undefined ? window.performance : Date;
function step(state, now) {
    var cur = state.animation.current;
    if (cur === undefined) {
        if (!state.dom.destroyed)
            state.dom.redrawNow();
        return;
    }
    var rest = 1 - (now - cur.start) * cur.frequency;
    if (rest <= 0) {
        if (cur.plan.nextPlan && (!isObjectEmpty(cur.plan.nextPlan.anims) || !isObjectEmpty(cur.plan.nextPlan.fadings))) {
            state.animation.current = {
                start: perf.now(),
                frequency: 2.2 / state.animation.duration,
                plan: cur.plan.nextPlan,
                lastMove: state.lastMove
            };
            cur = state.animation.current;
            rest = 1 - (perf.now() - cur.start) * cur.frequency;
        }
        else
            state.animation.current = undefined;
    }
    if (state.animation.current !== undefined) {
        var ease = easing(rest);
        for (var i in cur.plan.anims) {
            var cfg = cur.plan.anims[i];
            cfg[2] = cfg[0] * ease;
            cfg[3] = cfg[1] * ease;
        }
        state.dom.redrawNow(true);
        util.raf(function (now) {
            if (now === void 0) { now = perf.now(); }
            return step(state, now);
        });
    }
    else
        state.dom.redrawNow();
}
function animate(mutation, state, fadeOnly, noCaptSequences) {
    if (fadeOnly === void 0) { fadeOnly = false; }
    if (noCaptSequences === void 0) { noCaptSequences = false; }
    var prevPieces = __assign({}, state.pieces);
    var result = mutation(state);
    var plan = computePlan(prevPieces, state, fadeOnly, noCaptSequences);
    if (!isObjectEmpty(plan.anims) || !isObjectEmpty(plan.fadings)) {
        var alreadyRunning = state.animation.current && state.animation.current.start;
        state.animation.current = {
            start: perf.now(),
            frequency: ((plan.nextPlan && (!isObjectEmpty(plan.nextPlan.anims) || !isObjectEmpty(plan.nextPlan.fadings))) ? 2.2 : 1) / state.animation.duration,
            plan: plan,
            lastMove: state.lastMove
        };
        if (!alreadyRunning)
            step(state, perf.now());
    }
    else {
        if (state.animation.current && !sameArray(state.animation.current.lastMove, state.lastMove))
            state.animation.current = undefined;
        state.dom.redraw();
    }
    return result;
}
function sameArray(ar1, ar2) {
    if (!ar1 && !ar2)
        return true;
    if (!ar1 || !ar2 || ar1.length !== ar2.length)
        return false;
    for (var i = 0; i < ar1.length; i++) {
        if (ar1[i] !== ar2[i])
            return false;
    }
    return true;
}
function isObjectEmpty(o) {
    for (var _ in o)
        return false;
    return true;
}
exports.isObjectEmpty = isObjectEmpty;
function easing(t) {
    return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
}
