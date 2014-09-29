var m = require('mithril');
var partial = require('lodash-node/modern/functions/partial');
var chessground = require('chessground');
var data = require('./data');
var round = require('./round');
var socket = require('./socket');
var util = require('./util');

module.exports = function(cfg, router, i18n) {

  this.data = data(cfg);

  this.chessground = new chessground.controller({
    fen: cfg.game.fen,
    orientation: this.data.player.color,
    turnColor: this.data.game.player,
    lastMove: util.str2move(this.data.game.lastMove),
    movable: {
      free: false,
      color: round.isPlayerPlaying(this.data) ? this.data.player.color : null,
      dests: round.parsePossibleMoves(this.data.possibleMoves),
      events: {
        after: this.userMove
      },
    },
    animation: {
      enabled: true,
      duration: this.data.pref.animationDuration
    },
    premovable: {
      enabled: this.data.pref.enablePremove
    }
  });

  this.socket = window.lichess.socket = socket.make(this.data);

  this.userMove = function(orig, dest) {
    console.log('userMove', [orig, dest]);
  }.bind(this);

  this.router = router;

  this.costly = function(cell) {
    return (this.chessground.data.draggable.current.orig || this.chessground.data.animation.current.start) ? {
      subtree: 'retain'
    } : cell();
  }.bind(this);

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
