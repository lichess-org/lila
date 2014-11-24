var Chess = require('chessli.js').Chess;

function make(fen) {
  return new Chess(fen);
}

function move(c, m) {
  var m2 = {
    from: m[0],
    to: m[1]
  };
  if (m[2]) m2.promotion = m[2];
  c.move(m2);
}

function parseMove(m) {
  return m ? [m.from, m.to] : null;
}

function lastMove(c) {
  var hist = c.history({
    verbose: true
  });
  return parseMove(hist[hist.length - 1]);
}

module.exports = {
  make: make,
  move: move,
  parseMove: parseMove,
  lastMove: lastMove
};
