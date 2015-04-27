var game = require('game').game;
var status = require('game').status;
var xhr = require('../xhr');

module.exports = function(root) {

  this.root = root;
  this.active = false;
  this.broken = false;
  this.ply = 0;

  this.vm = {
    hash: ''
  };

  var showFen = function() {
    var s = root.data.game.situations[this.ply - 1];
    root.chessground.set({
      fen: s.fen,
      lastMove: s.uci ? [s.uci.substr(0, 2), s.uci.substr(2)] : null,
      check: s.check,
      turnColor: this.ply % 2 === 0 ? 'white' : 'black'
    });
  }.bind(this);

  var enable = function() {
    root.chessground.stop();
  }.bind(this);

  var disable = function() {
    this.ply = 0;
    root.chessground.set({
      movable: {
        color: game.isPlayerPlaying(root.data) ? root.data.player.color : null,
        dests: game.parsePossibleMoves(root.data.possibleMoves)
      }
    });
  }.bind(this);

  var onBreak = function() {
    disable();
    this.active = false;
    this.broken = true;
    xhr.reload(root).then(root.reload);
  }.bind(this);

  this.jump = function(ply) {
    if (this.broken) return;
    if (this.ply == ply || ply < 1 || ply > root.data.game.situations.length) return;
    this.active = ply != root.data.game.situations.length;
    this.ply = ply;
    showFen();
    if (this.active) enable();
    else disable();
  }.bind(this);

  this.situationsHash = function(sits) {
    var h = '';
    for (i in sits) {
      h += sits[i].lm;
    }
    return h;
  };

  this.onReload = function(cfg) {
    if (this.active && this.situationsHash(cfg.game.situations) != this.situationsHash(root.data.game.situations)) this.active = false;
  }.bind(this);

  this.enabledByPref = function() {
    var d = root.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited')
    );
  }.bind(this);

  this.isLate = function() {
    return this.active && status.playing(root.data);
  }.bind(this);
}
