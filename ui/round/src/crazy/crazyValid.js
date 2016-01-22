var game = require('game').game;

module.exports = {

  drop: function(chessground, data, role, key) {

    if (!game.isPlayerTurn(data)) return false;

    if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

    var dropStr = data.possibleDrops;

    if (typeof dropStr === 'undefined' || dropStr === null) return true;

    var drops = dropStr.match(/.{2}/g) || [];

    return drops.indexOf(key) !== -1;
  }
};
