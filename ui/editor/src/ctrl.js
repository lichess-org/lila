var partial = require('lodash-node/modern/functions/partial');
var chessground = require('chessground');
var editor = require('./editor');

module.exports = function(cfg) {

  this.data = editor.init(cfg);

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
    },
    draggable: {
      showGhost: false
    }
  });

  this.computeFen = partial(editor.computeFen, this.data, this.chessground.data);

  this.trans = partial(editor.trans, this.data);

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
    window.location = editor.makeUrl.call(this.data, fen);
  }.bind(this);
};
