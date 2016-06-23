var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeChess = require('./chess');
var makeGround = require('./ground').make;

module.exports = function(data) {

  var items = makeItems(data.items);
  var points = 0;
  var nbMoves = 0;

  var chess = makeChess(data.fen);
  var chessground = makeGround({
    chess: chess,
    orientation: data.color,
    onMove: onMove,
    items: {
      render: function(pos, key) {
        return items.withItem(key, itemView);
      }
    }
  });

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
      }
    });
    chess.color(stage.color);
    chessground.color(ground, stage.color, chess.dests());
  };

  return {
    data: data,
    chessground: chessground,
    points: 0,
    items: items
  };
};
