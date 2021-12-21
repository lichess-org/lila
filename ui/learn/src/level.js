var m = require('mithril');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeScenario = require('./scenario');
var makeShogi = require('./chess');
var ground = require('./ground');
var scoring = require('./score');
var sound = require('./sound');
var promotion = require('./promotion');
const timeouts = require('./timeouts');
var compat = require('shogiops/compat');
var util = require('./util');

module.exports = function (blueprint, opts) {
  var items = makeItems({
    apples: blueprint.apples,
  });

  var vm = {
    lastStep: false,
    completed: false,
    willComplete: false,
    failed: false,
    score: 0,
    nbMoves: 0,
  };

  var complete = function () {
    vm.willComplete = true;
    timeouts.setTimeout(
      function () {
        vm.lastStep = false;
        vm.completed = true;
        sound.levelEnd();
        vm.score += scoring.getLevelBonus(blueprint, vm.nbMoves);
        ground.stop();
        m.redraw();
        if (!blueprint.nextButton) timeouts.setTimeout(opts.onComplete, 1200);
      },
      ground.data().stats.dragged ? 1 : 250
    );
  };

  // cheat
  Mousetrap.bind(['shift+enter'], complete);

  var assertData = function () {
    return {
      scenario: scenario,
      shogi: shogi,
      vm: vm,
    };
  };

  var detectFailure = function () {
    var failed = blueprint.failure && blueprint.failure(assertData());
    if (failed) sound.failure();
    return failed;
  };

  var detectSuccess = function () {
    if (blueprint.success) return blueprint.success(assertData());
    else return !items.hasItem('apple');
  };

  var detectCapture = function () {
    if (!blueprint.detectCapture) return false;
    var fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
    var move = shogi[fun]();
    if (!move) return;
    vm.failed = true;
    ground.stop();
    if (!scenario.failedMovesPlayed()) {
      ground.showCapture(
        move,
        function () {
          shogi.move(move.orig, move.dest);
        },
        m
      );
    }
    sound.failure();
    return true;
  };

  var detectNifu = function (color, dest) {
    var nifu = shogi.findNifu(color, dest);
    if (!nifu) return;
    ground.stop();
    ground.showNifu([nifu.pos, dest]);
    sound.failure();
    return true;
  };

  var detectCapturedLessValuablePiece = function () {
    var square = shogi.findCapturedLessValuablePiece();
    if (!square) return;
    ground.stop();
    ground.setShapes([util.circle(square, 'green')]);
    sound.failure();
    return true;
  };
  // if orig is 'a0' then piece was dropped
  // to future self or anyone who wants it: the sendMove function is where you will implement feature for opponent's movement. also see scenario feature.
  var sendMove = function (orig, dest, prom, role) {
    vm.nbMoves++;
    var move = orig === 'a0' ? shogi.drop(role, dest) : shogi.move(orig, dest, prom);
    if (!move || !shogi.isCheck(blueprint.color)) ground.fen(shogi.fen(), blueprint.color, {});
    else {
      // moving into check
      vm.failed = true;
      ground.showCheckmate(shogi);
      sound.failure();
      return m.redraw();
    }
    var took = false,
      inScenario,
      captured = false,
      nifued = false,
      scenarioResult = false,
      notCapturedInOrder = false;
    items.withItem(move.to, function (item) {
      if (item === 'apple') {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      }
    });
    if (!took && move.captured && blueprint.pointsForCapture) {
      if (blueprint.showPieceValues) {
        vm.score += scoring.pieceValue(move.captured);
      } else {
        vm.score += scoring.capture;
      }
      took = true;
    }
    ground.check(shogi);
    var scenarioData = {
      move:
        (move.from === 'a0' ? compat.roleToLishogiChar(move.role) + '*' : move.from) +
        move.to +
        (move.promotion ? '+' : ''),
      complete: complete,
    };
    scenarioResult = scenario.player(scenarioData);
    if (scenarioResult === true) {
      vm.score += scoring.scenario;
      inScenario = true;
    } else {
      captured = detectCapture();
      if (role === 'pawn') nifued = detectNifu(blueprint.color, dest);
      if (blueprint.capturePiecesInOrderOfValue) notCapturedInOrder = detectCapturedLessValuablePiece();
      // see checkmate1.js for an example of typeof(scenarioResult) === string being true. the scenarioResult variable will be set to levelFail if any of the moves in the particular scenario are played
      vm.failed =
        vm.failed || typeof scenarioResult === 'string' || captured || nifued || notCapturedInOrder || detectFailure();
    }
    if (!vm.failed && detectSuccess()) complete();
    if (vm.willComplete) {
      ground.data().drawable.piece = undefined;
      return;
    }
    if (took) sound.take();
    //else if (inScenario) sound.take();
    else sound.move();
    if (vm.failed) {
      if (blueprint.showFailureFollowUp && !captured) {
        timeouts.setTimeout(function () {
          var rm = shogi.playRandomMove();
          if (rm) {
            ground.fen(shogi.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
          }
        }, 600);
      }
    } else {
      ground.select(dest);
      if (!inScenario) {
        shogi.color(blueprint.color);
        ground.color(blueprint.color, makeShogiDests());
        ground.data().dropmode.dropDests = shogi.getDropDests();
        // special case for drop.js level 1
        if (blueprint.highlightTakenPieceInPocket && move.captured) {
          ground.data().drawable.piece = {
            role: move.captured.role,
            color: move.captured.color === 'gote' ? 'sente' : 'gote',
          };
          ground.setShapes(blueprint.highlightTakenPieceInPocket);
        }
      }
    }
    m.redraw();
  };

  var makeShogiDests = function () {
    return shogi.dests({
      illegal: blueprint.offerIllegalMove,
    });
  };

  var onMove = function (orig, dest) {
    var piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var onDrop = function (piece, dest) {
    if (!piece || piece.color !== blueprint.color) return;
    sendMove('a0', dest, undefined, piece.role);
  };

  var shogi = makeShogi(blueprint.fen, blueprint.emptyApples ? [] : items.appleKeys());

  var scenario = makeScenario(blueprint.scenario, {
    shogi: shogi,
    makeShogiDests: makeShogiDests,
  });

  promotion.reset();

  ground.set({
    shogi: shogi,
    offerIllegalMove: blueprint.offerIllegalMove,
    autoCastle: blueprint.autoCastle,
    orientation: blueprint.color,
    onMove: onMove,
    onDrop: onDrop,
    items: {
      render: function (pos, key) {
        return items.withItem(key, itemView);
      },
    },
    shapes: blueprint.shapes,
    events: blueprint.events,
    lastMoves: blueprint.lastMoves,
    notation: document.getElementsByClassName('notation-0')[0] ? 0 : 1,
    dropmode: {
      showDropDests: true, //opts.something
      dropDests: shogi.getDropDestsIgnoreChecksAndNifu(blueprint.color),
    },
  });

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    pockets: !blueprint.noPocket && shogi.pockets(),
    scenario: scenario,
    start: function () {
      sound.levelStart();
      if (shogi.color() !== blueprint.color) timeouts.setTimeout(scenario.opponent, 1000);
    },
    onComplete: opts.onComplete,
    complete: complete,
  };
};
