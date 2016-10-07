var initialFen = require('../util').initialFen;

var eval = 15;

// lines selected using lichess opening explorer most popular moves
// best moves generated with Stockfish 7 at depth 25
var dict = {
  '': 'e2e4 e7e6 d2d4 d7d5 b1d2 g8f6 e4e5 f6d7 f1d3 c7c5',
  'e2e4': 'e7e5 g1f3 b8c6 f1b5 g8f6 e1g1 f8e7 f1e1 d7d6 d2d4',
  'e2e4 e7e5': 'g1f3 b8c6 f1c4 g8f6 d2d3 f8e7 c1d2 e8g8 e1g1 d7d6',
  'e2e4 e7e5 g1f3': 'b8c6 f1b5 a7a6 b5c6 d7c6 e1g1 f8d6 d2d4 e5d4 d1d4',
  'e2e4 e7e5 g1f3 b8c6': 'f1c4 g8f6 d2d3 f8c5 b1c3 e8g8 e1g1 d7d6 c1g5 h7h6',
  'e2e4 c7c5': 'g1f3 e7e6 b1c3 b8c6 f1b5 g8e7 d2d4 c6d4 f3d4 c5d4',
  'e2e4 c7c5 g1f3': 'd7d6 d2d4 c5d4 f3d4 g8f6 b1c3 b8c6 c1g5 e7e6 d1d2',
  'e2e4 c7c5 g1f3 d7d6': 'd2d4 c5d4 f3d4 g8f6 b1c3 b8c6 c1g5 e7e6 d1d2 f8e7',
  'e2e4 c7c5 g1f3 b8c6': 'd2d4 c5d4 f3d4 e7e5 d4b5 d7d6 b1a3 a7a6 b5c3 b7b5',
  'e2e4 e7e6': 'd2d4 d7d5 b1d2 c7c5 g1f3 g8f6 e4d5 e6d5 f1b5 c8d7',
  'e2e4 e7e6 d2d4': 'd7d5 b1d2 g8f6 e4e5 f6d7 c2c3 c7c5 f1d3 b7b6 g1e2',
  'e2e4 e7e6 d2d4 d7d5': 'b1d2 g8f6 e4e5 f6d7 c2c3 c7c5 f1d3 b7b6 g1e2 f8e7',
  'd2d4': 'd7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8b4 c1g5 e8g8 e2e3',
  'd2d4 d7d5': 'c2c4 e7e6 b1c3 f8e7 c1f4 g8f6 g1f3 e8g8 a2a3 b8d7',
  'd2d4 g8f6': 'c2c4 e7e6 g1f3 b7b6 a2a3 f8e7 b1c3 d7d5 c4d5 f6d5',
  'g1f3': 'd7d5 d2d4 c8f5 c2c4 e7e6 b1c3 b8c6 c4d5 e6d5 a2a3'
};

module.exports = function(work, variant, multiPv) {
  if (variant.key !== 'standard') return;
  if (multiPv > 1) return;
  if (work.initialFen === initialFen && work.moves.length <= 4) {
    var pv = dict[work.moves.join(' ')];
    if (pv) return {
      cp: eval,
      best: pv.split(' ')[0],
      pvs: [{
        cp: eval,
        best: pv.split(' ')[0],
        pv: pv
      }]
    };
  }
};
