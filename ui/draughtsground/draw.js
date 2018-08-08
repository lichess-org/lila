"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var board_1 = require("./board");
var util_1 = require("./util");
var brushes = ['green', 'red', 'blue', 'yellow'];
function start(state, e) {
    if (e.touches && e.touches.length > 1)
        return;
    e.stopPropagation();
    e.preventDefault();
    e.ctrlKey ? board_1.unselect(state) : board_1.cancelMove(state);
    var position = util_1.eventPosition(e);
    var orig = board_1.getKeyAtDomPos(position, state.orientation === 'white', state.dom.bounds());
    if (!orig)
        return;
    state.drawable.current = {
        orig: orig,
        pos: position,
        brush: eventBrush(e)
    };
    processDraw(state);
}
exports.start = start;
function processDraw(state) {
    util_1.raf(function () {
        var cur = state.drawable.current;
        if (cur) {
            var mouseSq = board_1.getKeyAtDomPos(cur.pos, state.orientation === 'white', state.dom.bounds());
            if (mouseSq !== cur.mouseSq) {
                if (!mouseSq) {
                    cur.prev = cur.mouseSq;
                    cur.mouseSq = undefined;
                }
                else {
                    cur.mouseSq = mouseSq;
                    cur.prev = undefined;
                    cur.dest = mouseSq !== cur.orig ? mouseSq : undefined;
                }
                state.dom.redrawNow();
            }
            processDraw(state);
        }
    });
}
exports.processDraw = processDraw;
function move(state, e) {
    if (state.drawable.current)
        state.drawable.current.pos = util_1.eventPosition(e);
}
exports.move = move;
function end(state) {
    var cur = state.drawable.current;
    if (cur) {
        if (cur.mouseSq)
            addShape(state.drawable, cur);
        cancel(state);
    }
}
exports.end = end;
function cancel(state) {
    if (state.drawable.current) {
        state.drawable.current = undefined;
        state.dom.redraw();
    }
}
exports.cancel = cancel;
function clear(state) {
    if (state.drawable.shapes.length) {
        state.drawable.shapes = [];
        state.dom.redraw();
        onChange(state.drawable);
    }
}
exports.clear = clear;
function eventBrush(e) {
    var a = e.shiftKey && util_1.isRightButton(e) ? 1 : 0;
    var b = e.altKey ? 2 : 0;
    return brushes[a + b];
}
function not(f) {
    return function (x) { return !f(x); };
}
function addShape(drawable, cur) {
    var sameShape = function (s) {
        return s.orig === cur.orig && s.dest === cur.dest;
    };
    var similar = drawable.shapes.filter(sameShape)[0];
    if (similar)
        drawable.shapes = drawable.shapes.filter(not(sameShape));
    if (!similar || similar.brush !== cur.brush)
        drawable.shapes.push(cur);
    onChange(drawable);
}
function onChange(drawable) {
    if (drawable.onChange)
        drawable.onChange(drawable.shapes);
}
