var chessground = require('chessground');
var partial = chessground.util.partial;
var editor = require('./editor');
var m = require('mithril');
var keyboard = require('./keyboard');

module.exports = function(cfg) {

  this.data = editor.init(cfg);

  this.trans = partial(editor.trans, this.data.i18n);

  this.vm = {
    redirecting: false
  };

  this.extraPositions = [{
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
    name: this.trans('startPosition')
  }, {
    fen: '8/8/8/8/8/8/8/8 w - -',
    name: this.trans('clearBoard')
  }, {
    fen: 'prompt',
    name: this.trans('loadPosition')
  }];

  this.positionIndex = {};
  cfg.positions.forEach(function(p, i) {
    this.positionIndex[p.fen.split(' ')[0]] = i;
  }.bind(this));

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
    drawable: {
      enabled: true
    },
    draggable: {
      showGhost: false,
      distance: 0,
      autoDistance: false
    },
    selectable: {
      enabled: false
    },
    events: {
      change: m.redraw
    },
    disableContextMenu: true
  });

  this.computeFen = partial(editor.computeFen, this.data, this.chessground.getFen);

  this.startPosition = function() {
    this.chessground.set({
      fen: 'start'
    });
    this.data.castles = editor.castlesAt(true);
    this.data.color('w');
  }.bind(this);

  this.clearBoard = function() {
    this.chessground.set({
      fen: '8/8/8/8/8/8/8/8'
    });
    this.data.castles = editor.castlesAt(false);
  }.bind(this);

  this.loadNewFen = function(fen) {
    if (fen === 'prompt') {
      fen = prompt('Paste FEN position').trim();
      if (!fen) return;
    }
    this.changeFen(fen);
  }.bind(this);

  this.changeFen = function(fen) {
    this.vm.redirecting = true;
    window.location = editor.makeUrl(this.data.baseUrl, fen);
  }.bind(this);

  this.positionLooksLegit = function() {
    var kings = {
      white: 0,
      black: 0
    };
    var pieces = this.chessground.data.pieces;
    for (var pos in pieces) {
      if (pieces[pos] && pieces[pos].role === 'king') kings[pieces[pos].color]++;
    }
    return kings.white === 1 && kings.black === 1;
  }.bind(this);

  keyboard(this);
};
