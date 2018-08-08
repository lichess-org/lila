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

  this.vm = {
    selected: m.prop('pointer'),
    redirecting: false
  };

  this.extraPositions = [{
      fen: 'W:W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20',
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
      fen: 'start'
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
      if (!fen) return;
    }
    this.changeFen(fen);
  }.bind(this);

  this.changeFen = function(fen) {
    this.vm.redirecting = true;
    window.location = editor.makeUrl(this.data.baseUrl, fen);
  }.bind(this);

  this.positionLooksLegit = function() {
    var pieces = this.draughtsground ? this.draughtsground.state.pieces : fenRead(this.cfg.fen);
    var totals = {
      white: 0,
      black: 0
    };
    for (var pos in pieces) {
        if (pieces[pos] && (pieces[pos].role === 'king' || pieces[pos].role === 'man')) {
            if (pieces[pos].role === 'man') {
                if (pieces[pos].color === 'white' && (pos === "01" || pos === "02" || pos === "03" || pos === "04" || pos === "05"))
                    return false;
                else if (pieces[pos].color === 'black' && (pos === "46" || pos === "47" || pos === "48" || pos === "49" || pos === "50"))
                    return false;
            }
            totals[pieces[pos].color]++;
        }
    }
    return totals.white !== 0 && totals.black !== 0 && (totals.white + totals.black) < 50;
  }.bind(this);

  this.setOrientation = function(o) {
    this.options.orientation = o;
    if (this.draughtsground.state.orientation !== o)
    this.draughtsground.toggleOrientation();
    m.redraw();
  }.bind(this);

  keyboard(this);
};
