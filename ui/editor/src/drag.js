var util = require('chessground').util;
var drag = require('chessground').drag;

module.exports = function(ctrl, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  var role = e.target.getAttribute('data-role'),
  color = e.target.getAttribute('data-color');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var key = util.allKeys.filter(function(k) {
    return !ctrl.chessground.data.pieces[k];
  })[0];
  if (!key) return;
  var isWhitePov = ctrl.chessground.data.orientation === 'white';
  var coords = util.key2pos(isWhitePov ? key : util.invertKey(key));
  var piece = {
    role: role,
    color: color
  };
  var obj = {};
  obj[key] = piece;
  ctrl.chessground.setPieces(obj);
  ctrl.chessground.data.render(); // ensure the new piece is in the DOM
  var bounds = ctrl.chessground.data.bounds();
  var squareBounds = e.target.parentNode.getBoundingClientRect();
  var pos = util.key2pos(key);
  var index = isWhitePov ? (8 - pos[1]) * 8 + pos[0] : (pos[1] - 1) * 8 + (9 - pos[0]);
  var pieceEl = ctrl.chessground.data.element.querySelector('square:nth-child(' + index + ') piece');
  pieceEl.classList.add('dragging');
  var rel = [
    (coords[0] - 1) * squareBounds.width + bounds.left,
    (8 - coords[1]) * squareBounds.height + bounds.top
  ];
  ctrl.chessground.data.draggable.current = {
    orig: key,
    piece: piece.color + piece.role,
    rel: rel,
    epos: [e.clientX, e.clientY],
    pos: [e.clientX - rel[0], e.clientY - rel[1]],
    dec: [-squareBounds.width / 2, -squareBounds.height / 2],
    bounds: bounds,
    started: true,
    pieceEl: pieceEl
  };
  drag.processDrag(ctrl.chessground.data);
}
