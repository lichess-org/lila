var m = require('mithril');
var game = require('game').game;
var ground = require('./ground');
var util = require('./util');
var xhr = require('./xhr');
var partial = require('chessground').util.partial;

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    takebackOffers: function(o) {
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      m.redraw();
    },
    move: function(o) {
      ctrl.apiMove(o);
    },
    reload: function(o) {
      xhr.reload(ctrl).then(ctrl.reload);
    },
    redirect: function() {
      ctrl.vm.redirecting = true;
      m.redraw();
    },
    clock: function(o) {
      if (ctrl.clock) ctrl.clock.update(o.white, o.black);
    },
    crowd: function(o) {
      ['white', 'black'].forEach(function(c) {
        game.setOnGame(ctrl.data, c, o[c]);
      });
      m.redraw();
    },
    end: function(winner) {
      ctrl.data.game.winner = winner;
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
