var range = require('lodash-node/modern/arrays/range');
var find = require('lodash-node/modern/collections/find');
var util = require('chessground').util;

module.exports = function(ctrl, e) {
  if (e.button !== 0) return; // only left click
  var role = e.target.getAttribute('data-role')
  color = e.target.getAttribute('data-color');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var key = find(util.allKeys, function(k) {
    return !ctrl.chessground.data.pieces[k];
  });
  if (!key) return;
  var coords = util.key2pos(key);
  var obj = {};
  obj[key] = {
    role: role,
    color: color
  };
  ctrl.chessground.setPieces(obj);
  var bounds = ctrl.chessground.data.bounds();
  var pieceBounds = e.target.getBoundingClientRect();
  var rel = [
    (coords[0] - 1) * pieceBounds.width + bounds.left,
    (8 - coords[1]) * pieceBounds.height + bounds.top
  ];
  ctrl.chessground.data.draggable.current = {
    orig: key,
    rel: rel,
    pos: [e.clientX - rel[0], e.clientY - rel[1]],
    dec: [-pieceBounds.width / 2, -pieceBounds.height / 2],
    bounds: bounds,
    started: true
  };
  m.redraw();
}
