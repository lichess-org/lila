var initialFen = require('../util').initialFen;

var eval = 15;

// lines selected using lichess opening explorer most popular moves
// best moves generated with Stockfish7 in one minute each
var dict = {
  '': 'e2e4',
  'e2e4': 'e7e5',
  'e2e4 e7e5': 'g1f3',
  'e2e4 e7e5 g1f3': 'b8c6',
  'e2e4 e7e5 g1f3 b8c6': 'f1c4',
  'e2e4 c7c5': 'g1f3',
  'e2e4 c7c5 g1f3': 'd7d6',
  'e2e4 c7c5 g1f3 d7d6': 'd2d4',
  'e2e4 c7c5 g1f3 b8c6': 'd2d4',
  'e2e4 e7e6': 'd2d4',
  'e2e4 e7e6 d2d4': 'd7d5',
  'e2e4 e7e6 d2d4 d7d5': 'b1d2',
  'd2d4': 'd7d5',
  'd2d4 d7d5': 'c2c4',
  'd2d4 g8f6': 'c2c4',
  'g1f3': 'd7d5'
};

module.exports = function(work) {
  if (work.position === initialFen && work.moves.length <= 4) {
    var best = dict[work.moves.join(' ')];
    if (best) return {
      cp: eval,
      best: best
    };
  }
};
