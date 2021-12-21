var util = require('./util');
var shogiopsUtil = require('shogiops/util');
var ground = require('./ground');
const timeouts = require('./timeouts');
var m = require('mithril');

var defaultDelay = 500;

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
  var failedMovesPlayed = false;

  var fail = function (moveSeq) {
    if (steps[it].wrongShapes) {
      ground.setShapes(steps[it].wrongShapes);
    }
    if (moveSeq) {
      failedMovesPlayed = true;
      ground.stop();
      for (var i = 1; i < moveSeq.length; i++) {
        var makeMoveWrapper = function (i) {
          return function () {
            makeMove(moveSeq[i]);
          };
        };
        timeouts.setTimeout(makeMoveWrapper(i), defaultDelay * i);
      }
    }
    isFailed = true;
    return false;
  };

  var makeMove = function (stepMove) {
    var res;
    var move = util.decomposeUci(stepMove);
    if (stepMove[1] === '*') {
      res = opts.shogi.drop(shogiopsUtil.charToRole(move[0][0]), move[1]);
    } else {
      res = opts.shogi.move(move[0], move[1], move[2]);
    }
    if (!res) fail();
    ground.fen(opts.shogi.fen(), opts.shogi.color(), opts.makeShogiDests(), move);
    ground.data().dropmode.dropDests = opts.shogi.getDropDests();
    m.redraw();
  };

  var opponent = function (data) {
    var step = steps[it];
    if (!step) return;
    var failure = makeMove(step.move);
    if (failure) return failure;
    it++;
    if (step.shapes)
      timeouts.setTimeout(function () {
        ground.setShapes(step.shapes);
      }, 70);

    if (it == steps.length) {
      ground.stop();
      timeouts.setTimeout(data.complete, defaultDelay);
    }
  };

  return {
    isComplete: function () {
      return it === steps.length;
    },
    isFailed: function () {
      return isFailed;
    },
    failedMovesPlayed: function () {
      return failedMovesPlayed;
    },
    opponent: opponent,
    player: function (data) {
      var move = data.move;
      var step = steps[it];
      if (!step) return;
      var moveMatcher = step.move;
      if (moveMatcher[moveMatcher.length - 1] === '/') {
        moveMatcher = moveMatcher.replace('/', '');
        if (move[move.length - 1] === '+') {
          moveMatcher += '+';
        }
      }
      if (moveMatcher !== move && !(Array.isArray(step.move) && step.move.includes(move))) {
        if (step.wrongMoves) {
          for (var moveSeq of step.wrongMoves) {
            if (moveSeq[0] === move || moveSeq[0] === 'any') {
              return fail(moveSeq);
            }
          }
        }
        return fail();
      }
      it++;
      if (step.shapes) ground.setShapes(step.shapes);
      if (step.levelFail) {
        return step.levelFail;
      }
      // example case in setup.js
      if (steps[it] && !steps[it].move) {
        it++;
        opts.shogi.color(shogiopsUtil.opposite(opts.shogi.color()));
        ground.color(opts.shogi.color(), opts.makeShogiDests());
      } else {
        var opponentWrapper = function () {
          opponent(data);
        };
        timeouts.setTimeout(opponentWrapper, steps[it] && steps[it].delay ? steps[it].delay : defaultDelay);
      }
      return true;
    },
  };
};
