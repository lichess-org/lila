var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeChess = require('./chess');
var ground = require('./ground');

module.exports = function(blueprint, opts) {

  var items = makeItems(blueprint.items);
  var points = 0;
  var nbMoves = 0;

  var onMove = function(orig, dest) {
    nbMoves++;
    var move = chess.move({
      from: orig,
      to: dest
    });
    if (!move) throw 'Invalid move!';
    items.withItem(move.to, function(item) {
      if (item.type === 'apple') {
        points += 100;
        items.remove(move.to);
      }
      if (item.type === 'flower' && !items.hasOfType('apple')) {
        points += 150;
        items.remove(move.to);
        opts.onComplete();
      }
    });
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

  return {
    blueprint: blueprint,
    chessground: chessground,
    points: 0,
    items: items
  };
};
