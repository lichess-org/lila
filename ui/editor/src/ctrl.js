var editor = require('./editor');
var m = require('mithril');
var keyboard = require('./keyboard');
var fenRead = require('chessground/fen').read;

module.exports = function(cfg) {

  this.cfg = cfg;
  this.data = editor.init(cfg);
  this.options = cfg.options || {};
  this.embed = cfg.embed;

  this.trans = lichess.trans(this.data.i18n);

  this.selected = m.prop('pointer');

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
  cfg.positions && cfg.positions.forEach(function(p, i) {
    this.positionIndex[p.fen.split(' ')[0]] = i;
  }.bind(this));

  this.chessground; // will be set from the view when instanciating chessground

  this.onChange = function() {
    this.options.onChange && this.options.onChange(this.computeFen());
    m.redraw();
  }.bind(this);

  this.computeFen = function() {
    return this.chessground ?
    editor.computeFen(this.data, this.chessground.getFen()) :
    cfg.fen;
  }.bind(this);

  this.bottomColor = function() {
    return this.chessground ?
    this.chessground.state.orientation :
    this.options.orientation || 'white';
  }.bind(this);

  this.setColor = function(letter) {
    this.data.color(letter);
    this.onChange();
  }.bind(this);

  this.setCastle = function(id, value) {
    this.data.castles[id](value);
    this.onChange();
  }.bind(this);

  this.startPosition = function() {
    this.chessground.set({
      fen: 'start'
    });
    this.data.castles = editor.castlesAt(true);
    this.data.color('w');
    this.onChange();
  }.bind(this);

  this.clearBoard = function() {
    this.chessground.set({
      fen: '8/8/8/8/8/8/8/8'
    });
    this.data.castles = editor.castlesAt(false);
    this.onChange();
  }.bind(this);

  this.loadNewFen = function(fen) {
    if (fen === 'prompt') {
      fen = prompt('Paste FEN position').trim();
      if (!fen) return;
    }
    this.changeFen(fen);
  }.bind(this);

  this.changeFen = function(fen) {
    window.location = editor.makeUrl(this.data.baseUrl, fen);
  }.bind(this);

  this.changeVariant = function(variant) {
    this.data.variant = variant;
    m.redraw();
  }.bind(this);

  this.positionLooksLegit = function() {
    var variant = this.data.variant;
    if (variant === "antichess") return true;
    var pieces = this.chessground ? this.chessground.state.pieces : fenRead(this.cfg.fen);
    var kings = {
      white: 0,
      black: 0
    };
    for (var pos in pieces) {
      if (pieces[pos] && pieces[pos].role === 'king') kings[pieces[pos].color]++;
    }
    return kings.white === (variant !== "horde" ? 1 : 0) && kings.black === 1;
  }.bind(this);

  this.setOrientation = function(o) {
    this.options.orientation = o;
    if (this.chessground.state.orientation !== o)
    this.chessground.toggleOrientation();
    m.redraw();
  }.bind(this);

  keyboard(this);
};
