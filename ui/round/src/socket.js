var m = require('mithril');
var game = require('game').game;
var ground = require('./ground');
var util = require('./util');
var xhr = require('./xhr');
var sound = require('./sound');
var partial = require('chessground').util.partial;

module.exports = function(send, ctrl) {

  this.send = send;

  this.sendLoading = function() {
    ctrl.setLoading(true);
    this.send.apply(this, arguments);
  }.bind(this);

  var handlers = {
    takebackOffers: function(o) {
      ctrl.setLoading(false);
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      m.redraw();
    },
    move: function(o) {
      o.isMove = true;
      ctrl.apiMove(o);
    },
    drop: function(o) {
      o.isDrop = true;
      ctrl.apiMove(o);
    },
    reload: function(o) {
      xhr.reload(ctrl).then(ctrl.reload);
    },
    redirect: function() {
      ctrl.setRedirecting();
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
      ctrl.setLoading(true);
      xhr.reload(ctrl).then(ctrl.reload);
      if (!ctrl.data.player.spectator && ctrl.data.game.turns > 1)
        $.sound[winner ? (ctrl.data.player.color === winner ? 'victory' : 'defeat') : 'draw']();
    },
    berserk: function(color) {
      ctrl.setBerserk(color);
    },
    gone: function(isGone) {
      if (!ctrl.data.opponent.ai) {
        game.setIsGone(ctrl.data, ctrl.data.opponent.color, isGone);
        m.redraw();
      }
    },
    checkCount: function(e) {
      ctrl.data.player.checks = ctrl.data.player.color == 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color == 'white' ? e.white : e.black;
      m.redraw();
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
        ctrl.setRedirecting(true);
        sound.move();
        lichess.hasToReload = true;
        location.href = '/' + gameId;
      }
    }
  };

  this.moreTime = util.throttle(300, false, partial(this.send, 'moretime', null));

  this.outoftime = util.throttle(500, false, partial(this.send, 'outoftime', null));

  this.berserk = util.throttle(200, false, partial(this.send, 'berserk', null));

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
}
