var m = require('mithril');
var round = require('./round');
var ground = require('chessground');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    possibleMoves: function(o) {
      ctrl.chessground.reconfigure({
        movable: {
          dests: round.parsePossibleMoves(o)
        }
      });
    },
    state: function(o) {
      ctrl.chessground.reconfigure({
        turnColor: o.color
      });
      ctrl.data.game.player = o.color;
      ctrl.data.game.turns = o.turns;
    },
    move: function(o) {
      ctrl.chessground.apiMove(o.from, o.to);
    },
    premove: function() {
      ctrl.chessground.playPremove();
    },
    castling: function(o) {
      var pieces = {};
      pieces[o.king[0]] = null;
      pieces[o.king[1]] = {
        role: 'king',
        color: o.color
      };
      pieces[o.rook[0]] = null;
      pieces[o.rook[1]] = {
        role: 'rook',
        color: o.color
      };
      ctrl.chessground.setPieces(pieces);
    },
    check: function(o) {
      ctrl.chessground.reconfigure({
        check: o
      });
    },
    enpassant: function(o) {
      var pieces = {};
      pieces.o = null;
      ctrl.chessground.setPieces(pieces);
    },
    // still used by rematch join
    redirect: function(o) {
      setTimeout(function() {
        lichess.hasToReload = true;
        $.redirect(o);
      }, 400);
    },
    threefoldRepetition: function() {
      // ???
    },
    clock: function(o) {
      if (ctrl.clock) ctrl.clock.update(o.white, o.black);
    }
  };

  this.receive = function(type, data) {
    console.log(type, data);
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
}
