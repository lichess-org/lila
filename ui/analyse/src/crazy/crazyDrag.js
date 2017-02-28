var util = require('chessground/util');
var drag = require('chessground/drag');

module.exports = function(ctrl, color, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.chessground.state.movable.color !== color) return;
  var role = e.target.getAttribute('data-role'),
    number = e.target.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  drag.dragNewPiece(ctrl.chessground.state, { color: color, role: role }, e);
}
