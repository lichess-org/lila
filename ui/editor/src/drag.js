var find = require('lodash/collection/find');
var util = require('chessground/util');
var drag = require('chessground/drag');

module.exports = function(ctrl, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (!pointerSelected(ctrl)) return;
  var role = e.target.getAttribute('data-role'),
  color = e.target.getAttribute('data-color');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var key = 'a0';
  var coords = util.key2pos(ctrl.chessground.state.orientation === 'white' ? key : util.invertKey(key));
  var piece = {
    role: role,
    color: color
  };
  var obj = {};
  obj[key] = piece;
  ctrl.chessground.setPieces(obj);
  var bounds = ctrl.chessground.state.bounds();
  var squareBounds = e.target.parentNode.getBoundingClientRect();
  var rel = [
    (coords[0] - 1) * squareBounds.width + bounds.left,
    (8 - coords[1]) * squareBounds.height + bounds.top
  ];
  ctrl.chessground.state.draggable.current = {
    orig: key,
    piece: piece.color + piece.role,
    rel: rel,
    epos: [e.clientX, e.clientY],
    pos: [e.clientX - rel[0], e.clientY - rel[1]],
    dec: [-squareBounds.width / 2, -squareBounds.height / 2],
    bounds: bounds,
    started: true,
    newPiece: true
  };
  drag.processDrag(ctrl.chessground.state);
}

function pointerSelected(ctrl) {
  return (!ctrl.chessground.sparePieceSelected || ctrl.chessground.sparePieceSelected === 'pointer');
}
