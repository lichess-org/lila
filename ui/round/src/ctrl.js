var m = require('mithril');
var throttle = require('lodash-node/modern/functions/throttle');
var chessground = require('chessground');
var partial = chessground.util.partial;
var data = require('./data');
var round = require('./round');
var status = require('./status');
var ground = require('./ground');
var socket = require('./socket');
var xhr = require('./xhr');
var promotion = require('./promotion');
var clockCtrl = require('./clock/ctrl');

module.exports = function(cfg, router, i18n, socketSend) {

  this.data = data(cfg);

  this.socket = new socket(socketSend, this);

  this.sendMove = function(orig, dest, prom) {
    var move = {
      from: orig,
      to: dest,
    };
    if (prom) move.promotion = prom;
    if (this.clock) move.lag = Math.round(lichess.socket.averageLag);
    this.socket.send('move', move, {
      ackable: true
    });
  }.bind(this);

  this.userMove = function(orig, dest, isPremove) {
    if (!promotion.start(this, orig, dest, isPremove)) this.sendMove(orig, dest);
  }.bind(this);

  this.chessground = ground.make(this.data, cfg.game.fen, this.userMove);

  this.reload = function(cfg) {
    this.data = data(cfg);
    ground.reload(this.chessground, this.data, cfg.game.fen);
  }.bind(this);

  this.clock = this.data.clock ? new clockCtrl(
    this.data.clock,
    throttle(partial(this.socket.send, 'outoftime'), 500)
  ) : false;

  this.isClockRunning = function() {
    return this.data.clock && round.playable(this.data) && 
      ((this.data.game.turns - this.data.game.startedAtTurn) > 1 || this.data.clock.running);
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
