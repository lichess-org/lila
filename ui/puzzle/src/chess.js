var groupBy = require('lodash-node/modern/collections/groupBy')
var mapValues = require('lodash-node/modern/objects/mapValues')
var Chess = require('chess.js').Chess;

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

function dests(c) {
  var moves = c.moves({
    verbose: true
  });
  var grouped = groupBy(moves.map(parseMove), function(m) {
    return m[0];
  });
  return mapValues(grouped, function(ms) {
    return ms.map(function(m) { return m[1]; });
  });
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
  dests: dests,
  lastMove: lastMove
};
