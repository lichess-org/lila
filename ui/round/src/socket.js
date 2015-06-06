var m = require('mithril');
var game = require('game').game;
var ground = require('./ground');
var atomic = require('./atomic');
var util = require('./util');
var xhr = require('./xhr');
var partial = require('chessground').util.partial;

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    possibleMoves: function(o) {
      ctrl.data.possibleMoves = o;
      if (!ctrl.replaying()) ctrl.chessground.set({
        movable: {
          dests: util.parsePossibleMoves(o)
        }
      });
    },
    state: function(o) {
      if (!ctrl.replaying()) ctrl.chessground.set({
        turnColor: o.color
      });
      ctrl.data.game.player = o.color;
      ctrl.data.game.turns = o.turns;
      if (o.status) ctrl.data.game.status = o.status;
      ctrl.data[ctrl.data.player.color === 'white' ? 'player' : 'opponent'].offeringDraw = o.wDraw;
      ctrl.data[ctrl.data.player.color === 'black' ? 'player' : 'opponent'].offeringDraw = o.bDraw;
      m.redraw();
      ctrl.setTitle();
    },
    takebackOffers: function(o) {
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      m.redraw();
    },
    move: function(o) {
      ctrl.apiMove(o);
    },
    premove: function() {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      if (ctrl.data.game.variant.key === 'atomic')
        setTimeout(ctrl.chessground.playPremove, 100);
      else ctrl.chessground.playPremove();
    },
    castling: function(o) {
      if (ctrl.replaying() || ctrl.chessground.data.autoCastle) return;
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
      if (!ctrl.replaying()) ctrl.chessground.set({
        check: o
      });
    },
    enpassant: function(o) {
      if (!ctrl.replaying()) {
        var pieces = {};
        pieces[o.key] = null;
        ctrl.chessground.setPieces(pieces);
        if (ctrl.data.game.variant.key === 'atomic')
          atomic.enpassant(ctrl, o.key, o.color);
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
    prefChange: function() {
      lichess.reload();
    },
    simulPlayerMove: function(gameId) {
      if (
        ctrl.userId &&
        ctrl.data.simul &&
        ctrl.userId == ctrl.data.simul.hostId &&
        gameId !== ctrl.data.game.id &&
        ctrl.moveOn.get() &&
        ctrl.chessground.data.turnColor !== ctrl.chessground.data.orientation) {
        $.sound.move();
        lichess.hasToReload = true;
        location.href = '/' + gameId;
      }
    }
  };

  this.moreTime = util.throttle(300, false, partial(send, 'moretime', null));

  this.outoftime = util.throttle(500, false, partial(send, 'outoftime', null));

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
}
