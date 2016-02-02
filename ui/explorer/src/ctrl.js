var chessground = require('chessground');
var m = require('mithril');

module.exports = function(cfg) {
  this.chessground = new chessground.controller({
  });

  this.api = m.request({
    method: 'GET',
    url: 'http://localhost:9000/bullet?fen=rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
  });
};
