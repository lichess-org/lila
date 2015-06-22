var Chess = require('chessli.js').Chess;

function make(fen) {
  return new Chess(fen);
}

function move(c, mo) {
  var m2 = {
    from: mo[0],
    to: mo[1]
  };
  if (mo[2]) m2.promotion = mo[2];
  c.move(m2);
}

function parseMove(mo) {
  return mo ? [mo.from, mo.to] : null;
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
