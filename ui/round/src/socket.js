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
      m.redraw();
      ctrl.setTitle();
    },
    move: function(o) {
      ctrl.chessground.apiMove(o.from, o.to);
      if (ctrl.data.game.threefold) ctrl.data.game.threefold = false;
      round.setOnGame(ctrl.data, o.color, true);
      m.redraw();
      $.sound.move(o.color == 'white');
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
      ctrl.data.game.threefold = true;
      m.redraw();
    },
    promotion: function(o) {
      ground.promote(ctrl.chessground, o.key, o.pieceClass);
    },
    clock: function(o) {
      if (ctrl.clock) ctrl.clock.update(o.white, o.black);
    },
    crowd: function(o) {
      ['white', 'black'].forEach(function(c) {
        round.setOnGame(ctrl.data, c, o[c]);
      });
      m.redraw();
    },
    end: function() {
      ground.end(ctrl.chessground);
      xhr.reload(ctrl.data).then(ctrl.reload);
      $.sound.dong();
    },
    gone: function(isGone) {
      if (!ctrl.data.opponent.ai) {
        round.setOnGame(ctrl.data, ctrl.data.opponent.color, !isGone);
        m.redraw();
      }
    },
  };

  this.receive = function(type, data) {
    // if (type != 'n') console.log(type, data);
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
}
