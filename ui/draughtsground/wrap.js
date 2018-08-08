"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var util_1 = require("./util");
var svg_1 = require("./svg");
var files = [46, 47, 48, 49, 50];
var filesBlack = [1, 2, 3, 4, 5];
var ranks = [5, 15, 25, 35, 45];
var ranksBlack = [6, 16, 26, 36, 46];
function wrap(element, s, bounds) {
    element.innerHTML = '';
    element.classList.add('cg-board-wrap');
    util_1.colors.forEach(function (c) {
        element.classList.toggle('orientation-' + c, s.orientation === c);
    });
    element.classList.toggle('manipulable', !s.viewOnly);
    var board = util_1.createEl('div', 'cg-board');
    element.appendChild(board);
    var svg;
    if (s.drawable.visible && bounds) {
        svg = svg_1.createElement('svg');
        svg.appendChild(svg_1.createElement('defs'));
        element.appendChild(svg);
    }
    if (s.coordinates) {
        if (s.orientation === 'black') {
            element.appendChild(renderCoords(ranksBlack, 'ranks black'));
            element.appendChild(renderCoords(filesBlack, 'files black'));
        }
        else {
            element.appendChild(renderCoords(ranks, 'ranks'));
            element.appendChild(renderCoords(files, 'files'));
        }
    }
    var ghost;
    if (bounds && s.draggable.showGhost) {
        ghost = util_1.createEl('piece', 'ghost');
        util_1.translateAway(ghost);
        element.appendChild(ghost);
    }
    return {
        board: board,
        ghost: ghost,
        svg: svg
    };
}
exports.default = wrap;
function renderCoords(elems, className) {
    var el = util_1.createEl('coords', className);
    var f;
    for (var i in elems) {
        f = util_1.createEl('coord');
        f.textContent = elems[i];
        el.appendChild(f);
    }
    return el;
}
