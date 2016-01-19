var game = require('game').game;

module.exports = {

  drop: function(chessground, possibleDrops, piece, pos) {

    if (piece.color !== chessground.data.movable.color) return false;

    if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

    if (typeof possibleDrops === 'undefined' || possibleDrops === null) return true;

    var drops = possibleDrops.match(/.{2}/g) || [];

    return drops.indexOf(pos) !== -1;
  }
};
