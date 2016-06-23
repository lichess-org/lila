var Chess = require('chess.js').Chess;

function chessToColor(chess) {
  return chess.turn() == "w" ? "white" : "black";
}

module.exports = function(fen) {

  var chess = new Chess(fen)

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
      return dests;
    },
    color: function(c) {
      if (c) chess.load(chess.fen().replace(/\s(w|b)\s/, c === 'white' ? ' w ' : ' b '));
      else return chessToColor(chess);
    },
    fen: chess.fen,
    move: chess.move
  };
};
