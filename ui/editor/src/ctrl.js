var editor = require('./editor');
var m = require('mithril');
var keyboard = require('./keyboard');
var fenRead = require('draughtsground/fen').read;

module.exports = function(cfg) {

  this.cfg = cfg;
  this.data = editor.init(cfg);
  this.options = cfg.options || {};
  this.embed = cfg.embed;

  this.trans = lidraughts.trans(this.data.i18n);

  this.selected = m.prop('pointer');

  this.extraPositions = [{
      fen: 'start',
      name: this.trans('startPosition')
  }, {
      fen: 'W:W:B',
      name: this.trans('clearBoard')
  }, {
      fen: 'prompt',
      name: this.trans('loadPosition')
  }];

  this.positionIndex = {};
  cfg.positions && cfg.positions.forEach(function(p, i) {
    this.positionIndex[p.fen.split(' ')[0]] = i;
  }.bind(this));

  this.draughtsground; // will be set from the view when instanciating draughtsground

  this.onChange = function() {
    this.options.onChange && this.options.onChange(this.computeFen());
    m.redraw();
  }.bind(this);

  this.computeFen = function() {
    return this.draughtsground ?
    editor.computeFen(this.data, this.draughtsground.getFen()) :
    cfg.fen;
  }.bind(this);

  this.bottomColor = function() {
    return this.draughtsground ?
    this.draughtsground.state.orientation :
    this.options.orientation || 'white';
  }.bind(this);

  this.setColor = function (letter) {
      this.data.color(letter.toLowerCase());
      this.onChange();
  }.bind(this);

  this.startPosition = function() {
    this.draughtsground.set({
      fen: this.data.variant.initialFen
    });
    this.data.color('w');
    this.onChange();
  }.bind(this);

  this.clearBoard = function() {
    this.draughtsground.set({
      fen: 'W:W:B'
    });
    this.onChange();
  }.bind(this);

  this.loadNewFen = function(fen) {
    if (fen === 'prompt') {
      fen = prompt('Paste FEN position').trim();
    } else if (fen === 'start') {
      fen = this.data.variant.initialFen;
    }
    if (fen) {
      this.changeFen(fen);
    }
  }.bind(this);

  this.changeFen = function(fen) {
    window.location = editor.makeUrl(this.data.baseUrl + (this.data.variant.key !== 'standard' ? this.data.variant.key + '/' : ''), fen);
  }.bind(this);

  this.changeVariant = function(key) {
    const variant = this.data.variants.find(v => v.key === key);
    if (variant) {
      this.data.variant = variant;
      this.draughtsground = undefined; // recreated from view
      m.redraw();
    }
  }.bind(this);

  this.positionLooksLegit = function() {
    const pieces = this.draughtsground ? this.draughtsground.state.pieces : fenRead(this.cfg.fen),
      totals = { white: 0, black: 0 },
      boardSize = this.data.variant.board.size,
      fields = boardSize[0] * boardSize[1] / 2,
      width = boardSize[0] / 2,
      backrankWhite = [], backrankBlack = [];
    for (let i = 1; i <= width; i++) {
      backrankWhite.push(i < 10 ? '0' +  i.toString() :  i.toString());
    }
    for (let i = fields - width + 1; i <= fields; i++) {
      backrankBlack.push(i < 10 ? '0' +  i.toString() :  i.toString());
    }
    for (let pos in pieces) {
      if (pieces[pos] && (pieces[pos].role === 'king' || pieces[pos].role === 'man')) {
        if (pieces[pos].role === 'man') {
          if (pieces[pos].color === 'white' && backrankWhite.includes(pos))
            return false;
          else if (pieces[pos].color === 'black' && backrankBlack.includes(pos))
            return false;
        }
        totals[pieces[pos].color]++;
      }
    }
    return totals.white !== 0 && totals.black !== 0 && (totals.white + totals.black) < fields;
  }.bind(this);

  this.setOrientation = function(o) {
    this.options.orientation = o;
    if (this.draughtsground.state.orientation !== o)
    this.draughtsground.toggleOrientation();
    m.redraw();
  }.bind(this);

  keyboard(this);
};
