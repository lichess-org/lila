"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.colors = ['white', 'black'];
exports.allKeys = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50"];
exports.pos2key = function (pos) { return exports.allKeys[pos[0] + 5 * pos[1] - 6]; };
exports.key2pos = function (k) { return key2posn(parseInt(k)); };
var key2posn = function (k) { return [(k - 1) % 5 + 1, ((k - 1) + (5 - (k - 1) % 5)) / 5]; };
function memo(f) {
    var v;
    var ret = function () {
        if (v === undefined)
            v = f();
        return v;
    };
    ret.clear = function () { v = undefined; };
    return ret;
}
exports.memo = memo;
exports.timer = function () {
    var startAt;
    return {
        start: function () { startAt = Date.now(); },
        cancel: function () { startAt = undefined; },
        stop: function () {
            if (!startAt)
                return 0;
            var time = Date.now() - startAt;
            startAt = undefined;
            return time;
        }
    };
};
exports.opposite = function (c) { return c === 'white' ? 'black' : 'white'; };
function containsX(xs, x) {
    return xs !== undefined && xs.indexOf(x) !== -1;
}
exports.containsX = containsX;
exports.distanceSq = function (pos1, pos2) {
    return Math.pow(pos1[0] - pos2[0], 2) + Math.pow(pos1[1] - pos2[1], 2);
};
exports.samePiece = function (p1, p2) {
    return p1.role === p2.role && p1.color === p2.color;
};
exports.computeIsTrident = function () { return window.navigator.userAgent.indexOf('Trident/') > -1; };
var posToTranslateBase = function (pos, asWhite, xFactor, yFactor, shift) {
    if (shift !== 0) {
        return [
            (!asWhite ? 4.5 - ((shift - 0.5) + pos[0]) : (shift - 0.5) + pos[0]) * xFactor,
            (!asWhite ? 10 - pos[1] : pos[1] - 1) * yFactor
        ];
    }
    else {
        return [
            (!asWhite ? 4.5 - ((pos[1] % 2 !== 0 ? -0.5 : -1) + pos[0]) : (pos[1] % 2 !== 0 ? -0.5 : -1) + pos[0]) * xFactor,
            (!asWhite ? 10 - pos[1] : pos[1] - 1) * yFactor
        ];
    }
};
exports.posToTranslateAbs = function (bounds) {
    var xFactor = bounds.width / 5, yFactor = bounds.height / 10;
    return function (pos, asWhite, shift) { return posToTranslateBase(pos, asWhite, xFactor, yFactor, shift); };
};
exports.posToTranslateRel = function (pos, asWhite, shift) { return posToTranslateBase(pos, asWhite, 20.0, 10.0, shift); };
exports.translateAbs = function (el, pos) {
    el.style.transform = "translate(" + pos[0] + "px," + pos[1] + "px)";
};
exports.translateRel = function (el, percents) {
    el.style.left = percents[0] + '%';
    el.style.top = percents[1] + '%';
};
exports.translateAway = function (el) { return exports.translateAbs(el, [-99999, -99999]); };
exports.eventPosition = function (e) {
    if (e.clientX || e.clientX === 0)
        return [e.clientX, e.clientY];
    if (e.touches && e.targetTouches[0])
        return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
    return undefined;
};
exports.isRightButton = function (e) { return e.buttons === 2 || e.button === 2; };
exports.createEl = function (tagName, className) {
    var el = document.createElement(tagName);
    if (className)
        el.className = className;
    return el;
};
exports.raf = (window.requestAnimationFrame || window.setTimeout).bind(window);
