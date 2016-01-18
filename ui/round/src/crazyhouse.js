var util = require('./util');
var game = require('game').game;

module.exports = {

  validateDrop: function(chessground, data, piece, pos) {

    if (!game.isPlayerTurn(data)) return false;

    if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

    var dropStr = data.possibleDrops;

    if (typeof dropStr === 'undefined' || dropStr === null) return true;

    var drops = dropStr.match(/.{2}/g) || [];

    return drops.indexOf(pos) !== -1;
  }
};
