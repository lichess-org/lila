var Chess = require('chess.js').Chess;
var round = require('./round');

module.exports = function(ctrl) {

  this.active = false;
  this.ply = 0;

  var showFen = function() {
    var moves = ctrl.data.game.moves;
    var chess = new Chess(ctrl.data.game.initialFen);
    var lastMove;
    for (var i = 0; i < this.ply; i++) {
      lastMove = chess.move(moves[i]);
    }
    ctrl.chessground.set({
      fen: chess.fen(),
      check: null,
      lastMove: [lastMove.from, lastMove.to],
      turnColor: this.ply % 2 === 0 ? 'white' : 'black'
    });
    if (chess.in_check()) ctrl.chessground.setCheck();
  }.bind(this);

  var enable = function() {
    ctrl.chessground.stop();
    showFen();
  }.bind(this);

  var disable = function() {
    showFen();
    ctrl.chessground.set({
      movable: {
        color: round.isPlayerPlaying(ctrl.data) ? ctrl.data.player.color : null,
        dests: round.parsePossibleMoves(ctrl.data.possibleMoves)
      }
    });
  }.bind(this);

  this.jump = function(ply) {
    this.active = ply != ctrl.data.game.moves.length;
    this.ply = ply;
    if (this.active) enable();
    else disable();
  }.bind(this);
}
