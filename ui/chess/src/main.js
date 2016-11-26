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
  }
};
