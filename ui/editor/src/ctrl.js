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

  this.computeFen = function() {
    var baseFen = chessground.fen.write(this.chessground.board.pieces.all);
    return baseFen + ' ' + data.fenMetadatas.call(this.editor);
  }.bind(this);

  this.trans = function(key) {
    return this.editor.i18n[key];
  }.bind(this);

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

  this.toggleOrientation = function() {
    this.chessground.toggleOrientation();
  }.bind(this);

  this.setColor = function(color) {
    this.editor.color = color;
  }.bind(this);

  this.setCastle = function(piece, available) {
    this.editor.castles[piece] = available;
  }.bind(this);

  this.loadNewFen = function(fen) {
    window.location = data.makeUrl.call(this.editor, fen);
  }.bind(this);
};
