var util = require('../util');

module.exports = {

  drop: function(chessground, possibleDrops, piece, pos) {

    if (piece.color !== chessground.data.movable.color) return false;

    if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

    var drops = util.readDrops(possibleDrops);

    if (drops === null) return true;

    return drops.indexOf(pos) !== -1;
  }
};
