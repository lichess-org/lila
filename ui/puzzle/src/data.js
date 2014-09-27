var merge = require('lodash-node/modern/objects/merge');
var chess = require('./chess');
var puzzle = require('./puzzle');
var util = require('chessground').util;

module.exports = function(cfg) {

  var data = {
    puzzle: {
      opponentColor: util.opposite(cfg.puzzle.color)
    },
    progress: [],
    chess: chess.make(cfg.puzzle.fen)
  };

  merge(data, cfg);

  data.puzzle.initialMove = puzzle.str2move(data.puzzle.initialMove);

  return data;
};
