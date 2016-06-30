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

  var addScore = function(v) {
    vm.score += v;
    opts.onScore(v);
  };

  var complete = function() {
    setTimeout(function() {
      if (vm.failed) return opts.restart();
      vm.lastStep = false;
      vm.completed = true;
      sound.levelEnd();
      var rank = scoring.getLevelRank(blueprint, vm.nbMoves);
      var bonus = scoring.getLevelBonus(rank);
      addScore(bonus);
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
    if (failed) sound.failure();
    return failed;
  };

  var detectSuccess = function() {
    if (blueprint.success) return blueprint.success.every(function(f) {
      return f(chess);
    });
    else return !items.hasItem('apple')
  };

  var sendMove = function(orig, dest, prom) {
    vm.nbMoves++;
    var move = chess.move(orig, dest, prom);
    if (!move) throw 'Invalid move!';
    var took = false;
    items.withItem(move.to, function(item) {
      if (item === 'apple') {
        addScore(scoring.apple);
        items.remove(move.to);
        took = true;
      }
    });
    if (move.captured) {
      addScore(scoring.capture);
      took = true;
    }
    vm.failed = vm.failed || detectFailure();
    if (!vm.failed && detectSuccess()) complete();
    if (vm.completed) return;
    if (took) sound.take();
    else sound.move();
    chess.color(blueprint.color);
    ground.color(blueprint.color, chess.dests());
    m.redraw();
  };

  var onMove = function(orig, dest) {
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

  if (blueprint.id !== 1) sound.levelStart();

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    restart: opts.restart
  };
};
