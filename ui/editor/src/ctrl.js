var chessground = require('chessground');
var data = require('./data');

module.exports = function(cfg) {

  this.editor = data.init(cfg);

  this.chessground = new chessground.controller({
    fen: cfg.fen,
    orientation: 'white',
    movable: {
      free: true,
      color: 'both',
      dropOff: 'trash'
    },
    premovable: {
      enabled: false
    }
  });

  this.computeFen = data.computeFen.bind(this.editor, this.chessground.board);

  this.trans = data.trans.bind(this.editor);

  this.startPosition = function() {
    this.chessground.reconfigure({
      fen: 'start'
    });
  }.bind(this);

  this.clearBoard = function() {
    this.chessground.reconfigure({
      fen: '8/8/8/8/8/8/8/8'
    });
  }.bind(this);

  this.loadNewFen = function(fen) {
    window.location = data.makeUrl.call(this.editor, fen);
  }.bind(this);
};
