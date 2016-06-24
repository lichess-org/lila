var m = require('mithril');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeChess = require('./chess');
var ground = require('./ground');
var sound = require('./sound');
var promotion = require('./promotion');

module.exports = function(blueprint, opts) {

  var items = makeItems(blueprint.items);

  var vm = {
    initialized: false,
    lastStep: false,
    completed: false,
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

  var getRank = function() {
    if (!vm.completed) return;
    var late = vm.nbMoves - blueprint.nbMoves;
    if (late <= 0) return 1;
    else if (late <= Math.max(1, blueprint.nbMoves / 8)) return 2;
    return 3;
  };

  var complete = function() {
    setTimeout(function() {
      vm.lastStep = false;
      vm.completed = true;
      sound.stageEnd();
      var rank = getRank();
      var bonus = 100;
      if (rank === 1) bonus = 500;
      else if (rank === 2) bonus = 300;
      addScore(bonus);
      ground.stop();
      m.redraw();
      setTimeout(opts.onComplete, 1200);
    }, ground.data().stats.dragged ? 0 : 250);
  };

  var sendMove = function(orig, dest, prom) {
    vm.nbMoves++;
    var move = chess.move(orig, dest, prom);
    if (!move) throw 'Invalid move!';
    var starTaken = false;
    items.withItem(move.to, function(item) {
      if (item === 'apple') {
        addScore(50);
        items.remove(move.to);
        starTaken = true;
      }
    });
    sound.move();
    if (starTaken) sound.take();
    update();
    if (vm.completed) {
      sound.end();
      return;
    }
    chess.color(blueprint.color);
    ground.color(blueprint.color, chess.dests());
  };

  var onMove = function(orig, dest) {
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var chess = makeChess(blueprint.fen);
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

  var update = function() {
    var hasApples = items.hasItem('apple');
    if (!hasApples) {
      if (ground.pieces()[items.flowerKey()]) complete();
      else vm.lastStep = true;
      m.redraw();
    }
  };
  update();

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    getRank: getRank
  };
};
