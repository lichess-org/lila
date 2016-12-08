var merge = require('merge');
var m = require('mithril');
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

  if (cfg.user) cfg.user.recent = cfg.user.recent || [];

  merge.recursive(data, cfg);

  data.puzzle.initialMove = puzzle.str2move(data.puzzle.initialMove);

  return data;
};
