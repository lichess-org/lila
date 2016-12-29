var piotr = require('./piotr');

module.exports = {

  initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',

  fixCrazySan: function(san) {
    return san[0] === 'P' ? san.slice(1) : san;
  },
  decomposeUci: function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  },

  renderEval: function(e) {
    e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
    return (e > 0 ? '+' : '') + e;
  },

  readDests: function(lines) {
    if (typeof lines === 'undefined') return null;
    var dests = {};
    if (lines) lines.split(' ').forEach(function(line) {
      dests[piotr[line[0]]] = line.split('').slice(1).map(function(c) {
        return piotr[c];
      });
    });
    return dests;
  },
  readDrops: function(line) {
    if (typeof line === 'undefined' || line === null) return null;
    return line.match(/.{2}/g) || [];
  },
  roleToSan: {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q',
    king: 'K'
  },
  sanToRole: {
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king'
  }
};
