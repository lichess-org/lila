var dragNewPiece = require('chessground/drag').dragNewPiece;
var readDrops = require('chess').readDrops;

module.exports = {
  drag: function(ctrl, color, e) {
    if (e.button !== undefined && e.button !== 0) return; // only touch or left click
    if (ctrl.chessground.state.movable.color !== color) return;
    var role = e.target.getAttribute('data-role'),
      number = e.target.getAttribute('data-nb');
    if (!role || !color || number === '0') return;
    e.stopPropagation();
    e.preventDefault();
    dragNewPiece(ctrl.chessground.state, { color: color, role: role }, e);
  },
  valid: function(chessground, possibleDrops, piece, pos) {

    if (piece.color !== chessground.state.movable.color) return false;

    if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

    var drops = readDrops(possibleDrops);

    if (drops === null) return true;

    return drops.indexOf(pos) !== -1;
  }
};
