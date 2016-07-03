var m = require('mithril');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
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
    var failed = false;
    (blueprint.failure || []).forEach(function(f) {
      failed = failed || f(chess);
    });
    if (failed) sound.once('failure', blueprint.id);
    return failed;
  };

  var detectSuccess = function() {
    if (blueprint.success) return blueprint.success.every(function(f) {
      return f(chess);
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
    if (!move) throw 'Invalid move!';
    var took = false;
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
    if (!vm.failed && detectSuccess()) complete();
    if (vm.completed) return;
    if (took) sound.take();
    else sound.move();
    if (vm.failed) {
      if (blueprint.showFailureFollowUp) setTimeout(function() {
        chess.playRandomMove();
        ground.fen(chess.fen(), blueprint.color, {});
      }, 600);
    } else {
      chess.color(blueprint.color);
      ground.color(blueprint.color, chess.dests());
    }
    m.redraw();
  };

  var onMove = function(orig, dest) {
    var piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var chess = makeChess(
    blueprint.fen,
    blueprint.emptyApples ? [] : items.appleKeys());

  ground.set({
    chess: chess,
    orientation: blueprint.color,
    onMove: onMove,
    items: {
      render: function(pos, key) {
        return items.withItem(key, itemView);
      }
    },
    shapes: blueprint.shapes
  });

  sound.levelStart();

  return {
    blueprint: blueprint,
    items: items,
    vm: vm
  };
};
