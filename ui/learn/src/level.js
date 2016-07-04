var m = require('mithril');
var util = require('./util');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeScenario = require('./scenario');
var makeChess = require('./chess');
var ground = require('./ground');
var scoring = require('./score');
var sound = require('./sound');
var promotion = require('./promotion');

module.exports = function(blueprint, opts) {

  var items = makeItems({
    apples: blueprint.apples
  });

  var vm = {
    initialized: false,
    lastStep: false,
    completed: false,
    failed: false,
    score: 0,
    nbMoves: 0
  };
  setTimeout(function() {
    vm.initialized = true;
    m.redraw();
  }, 100);

  var complete = function() {
    setTimeout(function() {
      vm.lastStep = false;
      vm.completed = true;
      sound.levelEnd();
      vm.score += scoring.getLevelBonus(blueprint, vm.nbMoves);
      ground.stop();
      m.redraw();
      setTimeout(opts.onComplete, 1200);
    }, ground.data().stats.dragged ? 0 : 250);
  };

  // cheat
  Mousetrap.bind(['shift+enter'], complete);

  var detectFailure = function() {
    var failed = blueprint.failure && blueprint.failure({
      scenario: scenario,
      chess: chess
    });
    if (failed) sound.once('failure', blueprint.id);
    return failed;
  };

  var detectSuccess = function() {
    if (blueprint.success) return blueprint.success({
      scenario: scenario,
      chess: chess
    });
    else return !items.hasItem('apple')
  };

  var detectCapture = function() {
    if (!blueprint.detectCapture) return false;
    var move = chess.findCapture();
    if (!move) return;
    vm.failed = true;
    ground.stop();
    ground.showCapture(move);
    sound.once('failure', blueprint.id);
    return true;
  };

  var sendMove = function(orig, dest, prom) {
    vm.nbMoves++;
    var move = chess.move(orig, dest, prom);
    ground.fen(chess.fen(), blueprint.color, {});
    if (!move) { // moving into check
      vm.failed = true;
      ground.showCheckmate(chess);
      return m.redraw();
    }
    var took = false,
      inScenario;
    items.withItem(move.to, function(item) {
      if (item === 'apple') {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      }
    });
    if (!took && move.captured && blueprint.pointsForCapture) {
      vm.score += scoring.capture;
      took = true;
    }
    vm.failed = vm.failed || detectFailure() || detectCapture();
    ground.check(chess);
    if (scenario.player(move.from + move.to + (move.promotion || ''))) {
      vm.score += scoring.scenario;
      inScenario = true;
    }
    vm.failed = vm.failed || detectFailure();
    if (!vm.failed && detectSuccess()) complete();
    if (vm.completed) return;
    if (took) sound.take();
    else if (inScenario) sound.take();
    else sound.move();
    if (vm.failed) {
      if (blueprint.showFailureFollowUp) setTimeout(function() {
        var rm = chess.playRandomMove();
        ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
      }, 600);
    } else if (!inScenario) {
      chess.color(blueprint.color);
      ground.color(blueprint.color, makeChessDests());
    }
    m.redraw();
  };

  var makeChessDests = function() {
    return chess.dests({
      illegal: blueprint.offerIllegalMove
    });
  };

  var onMove = function(orig, dest) {
    var piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var chess = makeChess(
    blueprint.fen,
    blueprint.emptyApples ? [] : items.appleKeys());

  var scenario = makeScenario(blueprint.scenario, {
    chess: chess,
    makeChessDests: makeChessDests
  });

  ground.set({
    chess: chess,
    offerIllegalMove: blueprint.offerIllegalMove,
    autoCastle: blueprint.autoCastle,
    orientation: blueprint.color,
    onMove: onMove,
    items: {
      render: function(pos, key) {
        return items.withItem(key, itemView);
      }
    },
    shapes: blueprint.shapes
  });

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    scenario: scenario,
    start: function() {
      sound.levelStart();
      setTimeout(scenario.opponent, 1000);
    }
  };
};
