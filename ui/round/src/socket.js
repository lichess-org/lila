var m = require('mithril');
var game = require('game').game;
var ground = require('./ground');
var atomic = require('./atomic');
var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    possibleMoves: function(o) {
      ctrl.data.possibleMoves = o;
      if (!ctrl.replay.active) ctrl.chessground.set({
        movable: {
          dests: game.parsePossibleMoves(o)
        }
      });
    },
    state: function(o) {
      if (!ctrl.replay.active) ctrl.chessground.set({
        turnColor: o.color
      });
      ctrl.data.game.player = o.color;
      ctrl.data.game.turns = o.turns;
      m.redraw();
      ctrl.setTitle();
    },
    move: function(o) {
      ctrl.apiMove(o);
    },
    premove: function() {
      ctrl.chessground.playPremove();
    },
    castling: function(o) {
      if (ctrl.replay.active || ctrl.chessground.data.autoCastle) return;
      var pieces = {};
      pieces[o.king[0]] = null;
      pieces[o.rook[0]] = null;
      pieces[o.king[1]] = {
        role: 'king',
        color: o.color
      };
      pieces[o.rook[1]] = {
        role: 'rook',
        color: o.color
      };
      ctrl.chessground.setPieces(pieces);
    },
    check: function(o) {
      if (!ctrl.replay.active) ctrl.chessground.set({
        check: o
      });
    },
    enpassant: function(o) {
      if (!ctrl.replay.active) {
        if (ctrl.data.game.variant.key === 'atomic')
          atomic.enpassant(ctrl, o.key, o.color);
        else {
          var pieces = {};
          pieces[o.key] = null;
          ctrl.chessground.setPieces(pieces);
        }
      }
      $.sound.take();
    },
    reload: function(o) {
      xhr.reload(ctrl).then(ctrl.reload);
    },
    redirect: function() {
      ctrl.vm.redirecting = true;
      m.redraw();
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
    cclock: function(o) {
      if (ctrl.correspondenceClock) ctrl.correspondenceClock.update(o.white, o.black);
    },
    crowd: function(o) {
      ['white', 'black'].forEach(function(c) {
        game.setOnGame(ctrl.data, c, o[c]);
      });
      m.redraw();
    },
    end: function() {
      ground.end(ctrl.chessground);
      xhr.reload(ctrl).then(ctrl.reload);
      if (!ctrl.data.player.spectator) $.sound.dong();
    },
    gone: function(isGone) {
      if (!ctrl.data.opponent.ai) {
        game.setIsGone(ctrl.data, ctrl.data.opponent.color, isGone);
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
