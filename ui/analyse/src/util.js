var fixCrazySan = require('chess').fixCrazySan;
var common = require('common');
var m = require('mithril');

var plyToTurn = function(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

module.exports = {
  synthetic: function(data) {
    return data.game.id === 'synthetic';
  },
  plyToTurn: plyToTurn,
  nodeFullName: function(node) {
    if (node.san) return plyToTurn(node.ply) + (
      node.ply % 2 === 1 ? '.' : '...'
    ) + ' ' + fixCrazySan(node.san);
    return 'Initial position';
  },
  plural: function(noun, nb) {
    return nb + ' ' + (nb === 1 ? noun : noun + 's');
  },
  titleNameToId: function(titleName) {
    var split = titleName.split(' ');
    var name = split.length == 1 ? split[0] : split[1];
    return name.toLowerCase();
  },
  roleToSan: {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q'
  },
  sanToRole: {
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen'
  }
};
