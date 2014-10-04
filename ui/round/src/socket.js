var m = require('mithril');
var round = require('./round');
var ground = require('./ground');
var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  this.send = send;

  var d = ctrl.data;

  var handlers = {
    possibleMoves: function(o) {
      ctrl.chessground.set({
        movable: {
          dests: round.parsePossibleMoves(o)
        }
      });
    },
    state: function(o) {
      ctrl.chessground.set({
        turnColor: o.color
      });
      d.game.player = o.color;
      d.game.turns = o.turns;
    },
    move: function(o) {
      ctrl.chessground.apiMove(o.from, o.to);
      if (d.game.threefold) {
        m.startComputation();
        d.game.threefold = false;
        m.endComputation();
      }
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
      ctrl.chessground.set({
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
    reload: function(o) {
      xhr.reload(d).then(ctrl.reload);
    },
    threefoldRepetition: function() {
      m.startComputation();
      d.game.threefold = true;
      m.endComputation();
    },
    clock: function(o) {
      if (ctrl.clock) ctrl.clock.update(o.white, o.black);
    },
    crowd: function(o) {
      m.startComputation();
      ['white', 'black'].forEach(function(c) {
        round.getPlayer(d, c).statused = true;
        round.getPlayer(d, c).connected = o[c];
      });
      d.watchers = o.watchers;
      m.endComputation();
    },
    end: function() {
      ground.end(ctrl.chessground);
      xhr.reload(d).then(ctrl.reload);
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
