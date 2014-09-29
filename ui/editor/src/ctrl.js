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
    animation: {
      duration: cfg.animation.duration
    },
    premovable: {
      enabled: false
    },
    draggable: {
      showGhost: false
    }
  });

  this.computeFen = partial(editor.computeFen, this.data, this.chessground.getFen);

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
    window.location = editor.makeUrl(this.data, fen);
  }.bind(this);

  this.chessgroundIsAnimating = function() {
    return this.chessground.data.draggable.current.orig || this.chessground.data.animation.current.start;
  }.bind(this);

  this.costly = function(cell) {
    return this.chessgroundIsAnimating() ? {
      subtree: 'retain'
    } : cell();
  }.bind(this);
};
