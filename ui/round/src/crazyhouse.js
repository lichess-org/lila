var util = require('./util');

module.exports = {

  validateDrop: function(chessground, dropStr, piece, pos) {

    if (piece.role === 'pawn' && (pos[1] === '1' || pos[1] === '8')) return false;

    if (typeof dropStr === 'undefined' || dropStr === null) return true;

    var drops = dropStr.match(/.{2}/g) || [];

    return drops.indexOf(pos) !== -1;
  }
};
