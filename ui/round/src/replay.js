var Chess = require('chess.js').Chess;
var round = require('./round');

module.exports = function(ctrl) {

  this.active = false;
  this.ply = 0;
  this.late = false;

  var situationCache = {};

  var computeSituation = function() {
    var ply, move, cached, fen, hash, h, lm;
    for (ply = 1; ply <= this.ply; ply++) {
      move = ctrl.data.game.moves[ply - 1];
      h += move;
      cached = situationCache[h];
      if (!cached) break;
      hash = h;
      fen = cached.fen;
    }
    if (cached && ply == this.ply) return cached;
    var chess = new Chess(fen || ctrl.data.game.initialFen);
    var moves = ctrl.data.game.moves.slice(ply - 1, this.ply - ply + 1);
    for (ply = ply; ply <= this.ply; ply++) {
      move = ctrl.data.game.moves[ply - 1];
      hash += move;
      lm = chess.move(move);
      situationCache[hash] = {
        fen: chess.fen(),
        check: chess.in_check(),
        lastMove: [lm.from, lm.to],
        turnColor: ply % 2 === 0 ? 'white' : 'black'
      };
    }
    return situationCache[hash];
  }.bind(this);

  var showFen = function() {
    ctrl.chessground.set(computeSituation());
  }.bind(this);

  var enable = function() {
    ctrl.chessground.stop();
    showFen();
  }.bind(this);

  var disable = function() {
    this.late = false;
    showFen();
    ctrl.chessground.set({
      movable: {
        color: round.isPlayerPlaying(ctrl.data) ? ctrl.data.player.color : null,
        dests: round.parsePossibleMoves(ctrl.data.possibleMoves)
      }
    });
  }.bind(this);

  this.jump = function(ply) {
    if (this.ply == ply || ply < 1 || ply > ctrl.data.game.moves.length) return;
    this.active = ply != ctrl.data.game.moves.length;
    this.ply = ply;
    if (this.active) enable();
    else disable();
  }.bind(this);

  this.onReload = function(cfg) {
    if (this.active && cfg.game.moves.join() != ctrl.data.game.moves.join()) this.active = false;
  }.bind(this);
}
