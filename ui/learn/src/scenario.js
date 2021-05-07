var util = require('./util');
var ground = require('./ground');
const timeouts = require('./timeouts');

module.exports = function (blueprint, opts) {
  var steps = (blueprint || []).map(function (step) {
    if (step.move) return step;
    return {
      move: step,
      shapes: [],
    };
  });

  var it = 0;
  var isFailed = false;

  var fail = function () {
    isFailed = true;
    return false;
  };

  var opponent = function () {
    var step = steps[it];
    if (!step) return;
    var move = util.decomposeUci(step.move);
    var res = opts.chess.move(move[0], move[1], move[2]);
    if (!res) return fail();
    it++;
    ground.fen(opts.chess.fen(), opts.chess.color(), opts.makeChessDests(), move);
    if (step.shapes)
      timeouts.setTimeout(function () {
        ground.setShapes(step.shapes);
      }, 500);
  };

  return {
    isComplete: function () {
      return it === steps.length;
    },
    isFailed: function () {
      return isFailed;
    },
    opponent: opponent,
    player: function (move) {
      var step = steps[it];
      if (!step) return;
      if (step.move !== move) return fail();
      it++;
      if (step.shapes) ground.setShapes(step.shapes);
      timeouts.setTimeout(opponent, 1000);
      return true;
    },
  };
};
