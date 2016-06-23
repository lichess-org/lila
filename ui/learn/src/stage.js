var m = require('mithril');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeChess = require('./chess');
var ground = require('./ground');

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

  var complete = function() {
    vm.lastStep = false;
    var late = vm.nbMoves - blueprint.nbMoves;
    if (late === 0) bonus = 500;
    else if (late === 1) bonus = 300;
    else if (late === 2) bonus = 200;
    else bonus = 100;
    addScore(bonus);
    vm.completed = true;
    chessground.stop();
    m.redraw();
    setTimeout(opts.onComplete, 1500);
  };

  var onMove = function(orig, dest) {
    vm.nbMoves++;
    var move = chess.move({
      from: orig,
      to: dest
    });
    if (!move) throw 'Invalid move!';
    items.withItem(move.to, function(item) {
      if (item.type === 'apple') {
        addScore(50);
        items.remove(move.to);
      } else if (item.type === 'flower' && !items.hasOfType('apple')) complete();
    });
    update();
    if (vm.completed) return;
    chess.color(blueprint.color);
    ground.color(chessground, blueprint.color, chess.dests());
  };

  var chess = makeChess(blueprint.fen);
  var chessground = ground.make({
    chess: chess,
    orientation: blueprint.color,
    onMove: onMove,
    items: {
      render: function(pos, key) {
        return items.withItem(key, itemView);
      }
    }
  });

  var update = function() {
    console.log(items.hasOfType('apple'));
    vm.lastStep = !items.hasOfType('apple');
    m.redraw();
  };
  update();

  return {
    blueprint: blueprint,
    chessground: chessground,
    items: items,
    vm: vm
  };
};
