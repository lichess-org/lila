var m = require('mithril');
var round = require('./round');
var ground = require('./ground');
var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  this.send = send;

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
      xhr.reload(ctrl.data).then(ctrl.reload);
    },
    threefoldRepetition: function() {
      // show the draw button
      xhr.reload(ctrl.data).then(ctrl.reload);
    },
    clock: function(o) {
      if (ctrl.clock) ctrl.clock.update(o.white, o.black);
    },
    crowd: function(o) {
      m.startComputation();
      ['white', 'black'].forEach(function(c) {
        round.getPlayer(ctrl.data, c).statused = true;
        round.getPlayer(ctrl.data, c).connected = o[c];
      });
      ctrl.data.watchers = o.watchers;
      m.endComputation();
    },
    end: function() {
      ground.end(ctrl.chessground);
      xhr.reload(ctrl.data).then(ctrl.reload);
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
