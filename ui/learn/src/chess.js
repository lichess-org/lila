var Chess = require('chess.js').Chess;

module.exports = function(fen) {

  var chess = new Chess(fen)

  function getColor() {
    return chess.turn() == "w" ? "white" : "black";
  }

  function setColor(c) {
    var turn = c === 'white' ? ' w ' : ' b ';
    var newFen = chess.fen().replace(/ (w|b) /, turn);
    chess.load(newFen);
    if (getColor() !== c) {
      // the en passant square prevents setting color
      newFen = newFen.replace(/ (w|b) - \w{2} /, turn + ' - - ');
      chess.load(newFen);
    }
  }

  return {
    dests: function() {
      var dests = {};
      chess.SQUARES.forEach(function(s) {
        var ms = chess.moves({
          square: s,
          verbose: true
        });
        if (ms.length) dests[s] = ms.map(function(m) {
          return m.to;
        });
      });
      console.log(chess.turn(), chess.fen(), chess.moves(), chess.ascii());
      return dests;
    },
    color: function(c) {
      if (c) setColor(c);
      else return getColor(c);
    },
    fen: chess.fen,
    move: chess.move
  };
};
