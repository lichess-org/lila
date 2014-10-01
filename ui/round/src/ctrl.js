var m = require('mithril');
var partial = require('lodash-node/modern/functions/partial');
var chessground = require('chessground');
var data = require('./data');
var round = require('./round');
var socket = require('./socket');
var clockCtrl = require('./clock/ctrl');
var util = require('./util');

module.exports = function(cfg, router, i18n, socketSend) {

  this.data = data(cfg);

  this.socket = new socket(socketSend, this);

  this.userMove = function(orig, dest) {
    var move = {
      from: orig,
      to: dest,
    };
    if (this.clock) move.lag = Math.round(lichess.socket.averageLag);
    this.socket.send('move', move, {
      ackable: true
    });
  }.bind(this);

  this.chessground = new chessground.controller({
    fen: cfg.game.fen,
    orientation: this.data.player.color,
    turnColor: this.data.game.player,
    lastMove: util.str2move(this.data.game.lastMove),
    highlight: {
      lastMove: this.data.pref.highlight,
      check: this.data.pref.highlight,
      dragOver: true
    },
    movable: {
      free: false,
      color: round.isPlayerPlaying(this.data) ? this.data.player.color : null,
      dests: round.parsePossibleMoves(this.data.possibleMoves),
      showDests: this.data.pref.destination,
      events: {
        after: this.userMove
      },
    },
    animation: {
      enabled: true,
      duration: this.data.pref.animationDuration
    },
    premovable: {
      enabled: this.data.pref.enablePremove,
      showDests: this.data.pref.destination
    }
  });

  this.clock = this.data.clock ? new clockCtrl(this.data.clock) : false;

  this.isClockRunning = function() {
    return !this.data.game.finished && ((this.data.game.turns - this.data.game.startedAtTurn) > 1 || this.data.game.clockRunning);
  }.bind(this);

  this.clockTick = function() {
    if (this.isClockRunning()) this.clock.tick(this.data.game.player);
  }.bind(this);

  if (this.clock) setInterval(this.clockTick, 100);

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
