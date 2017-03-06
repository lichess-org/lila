var util = require('chessground/util');

function capture(ctrl, key) {
  var exploding = [];
  var diff = {};
  var orig = util.key2pos(key);
  var minX = Math.max(1, orig[0] - 1),
      maxX = Math.min(8, orig[0] + 1),
      minY = Math.max(1, orig[1] - 1),
      maxY = Math.min(8, orig[1] + 1);

  for (var x = minX; x <= maxX; x++) {
    for (var y = minY; y <= maxY; y++) {
      var k = util.pos2key([x, y]);
      if (k) {
        exploding.push(k);
        var explodes = ctrl.chessground.state.pieces[k] && (
          k === key || ctrl.chessground.state.pieces[k].role !== 'pawn')
        if (explodes) diff[k] = null;
      }
    }
  }
  ctrl.chessground.setPieces(diff);
  ctrl.chessground.explode(exploding);
}

// needs to explicitly destroy the capturing pawn
function enpassant(ctrl, key, color) {
  var pos = util.key2pos(key);
  var pawnPos = [pos[0], pos[1] + (color === 'white' ? -1 : 1)];
  capture(ctrl, util.pos2key(pawnPos));
}

module.exports = {
  capture: capture,
  enpassant: enpassant
};
