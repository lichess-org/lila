var m = require('mithril');
var throttle = require('./util').throttle;
var chessground = require('chessground');
var partial = chessground.util.partial;
var data = require('./data');
var game = require('game').game;
var status = require('game').status;
var ground = require('./ground');
var socket = require('./socket');
var title = require('./title');
var promotion = require('./promotion');
var hold = require('./hold');
var blur = require('./blur');
var init = require('./init');
var blind = require('./blind');
var clockCtrl = require('./clock/ctrl');
var correspondenceClockCtrl = require('./correspondenceClock/ctrl');
var moveOn = require('./moveOn');
var atomic = require('./atomic');
var util = require('./util');

module.exports = function(opts) {

  this.data = data({}, opts.data);

  this.userId = opts.userId;

  this.firstPly = function() {
    return this.data.steps[0].ply;
  }.bind(this);

  this.lastPly = function() {
    return this.data.steps[this.data.steps.length - 1].ply;
  }.bind(this);

  this.plyStep = function(ply) {
    return this.data.steps[ply - this.firstPly()];
  }.bind(this);

  this.vm = {
    ply: this.lastPly(),
    flip: false,
    reloading: false,
    redirecting: false,
    replayHash: ''
  };

  this.socket = new socket(opts.socketSend, this);

  var onUserMove = function(orig, dest, meta) {
    hold.register(this.socket, meta.holdTime);
    if (!promotion.start(this, orig, dest, meta.premove)) this.sendMove(orig, dest);
  }.bind(this);

  var onMove = function(orig, dest, captured) {
    if (captured) {
      if (this.data.game.variant.key === 'atomic') {
        $.sound.explode();
        atomic.capture(this, dest, captured);
      } else $.sound.take();
    } else $.sound.move();
  }.bind(this);

  this.chessground = ground.make(this.data, opts.data.game.fen, onUserMove, onMove);

  this.replaying = function() {
    return this.vm.ply !== this.lastPly();
  }.bind(this);

  this.stepsHash = function(steps) {
    var h = '';
    for (i in steps) {
      h += steps[i].san;
    }
    return h;
  };

  this.jump = function(ply) {
    if (ply < this.firstPly() || ply > this.lastPly()) return;
    this.vm.ply = ply;
    var s = this.plyStep(ply);
    var config = {
      fen: s.fen,
      lastMove: s.uci ? [s.uci.substr(0, 2), s.uci.substr(2, 2)] : null,
      check: s.check,
      turnColor: this.vm.ply % 2 === 0 ? 'white' : 'black'
    };
    if (this.replaying()) this.chessground.stop();
    else config.movable = {
      color: game.isPlayerPlaying(this.data) ? this.data.player.color : null,
      dests: util.parsePossibleMoves(this.data.possibleMoves)
    }
    this.chessground.set(config);
  }.bind(this);

  this.replayEnabledByPref = function() {
    var d = this.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited')
    );
  }.bind(this);

  this.isLate = function() {
    return this.replaying() && status.playing(this.data);
  }.bind(this);

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.vm.flip ? this.data.opponent.color : this.data.player.color
    });
  }.bind(this);

  this.setTitle = partial(title.set, this);

  this.sendMove = function(orig, dest, prom) {
    var move = {
      from: orig,
      to: dest
    };
    if (prom) move.promotion = prom;
    if (blur.get()) move.b = 1;
    if (this.clock) move.lag = Math.round(lichess.socket.averageLag);
    this.socket.send('move', move, {
      ackable: true
    });
  }.bind(this);

  this.apiMove = function(o) {
    m.startComputation();
    if (!this.replaying()) {
      this.vm.ply++;
      this.chessground.apiMove(o.from, o.to);
    }
    if (this.data.game.threefold) this.data.game.threefold = false;
    this.data.steps.push({
      ply: this.lastPly() + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check
    });
    game.setOnGame(this.data, o.color, true);
    m.endComputation();
    if (this.data.blind) blind.reload(this);
    if (game.isPlayerPlaying(this.data) && o.color === this.data.player.color) this.moveOn.next();
  }.bind(this);

  this.reload = function(cfg) {
    m.startComputation();
    if (this.stepsHash(cfg.steps) !== this.stepsHash(this.data.steps))
      this.vm.ply = cfg.steps[cfg.steps.length - 1].ply;
    this.data = data(this.data, cfg);
    makeCorrespondenceClock();
    if (this.clock) this.clock.update(this.data.clock.white, this.data.clock.black);
    if (!this.replaying()) ground.reload(this.chessground, this.data, cfg.game.fen, this.vm.flip);
    this.setTitle();
    if (this.data.blind) blind.reload(this);
    this.moveOn.next();
    setQuietMode();
    m.endComputation();
  }.bind(this);

  this.clock = this.data.clock ? new clockCtrl(
    this.data.clock,
    this.socket.outoftime, (this.data.simul || this.data.player.spectator || !this.data.pref.clockSound) ? null : this.data.player.color
  ) : false;

  this.isClockRunning = function() {
    return this.data.clock && game.playable(this.data) &&
      ((this.data.game.turns - this.data.game.startedAtTurn) > 1 || this.data.clock.running);
  }.bind(this);

  var clockTick = function() {
    if (this.isClockRunning()) this.clock.tick(this.data.game.player);
  }.bind(this);

  var makeCorrespondenceClock = function() {
    if (this.data.correspondence && !this.correspondenceClock)
      this.correspondenceClock = new correspondenceClockCtrl(
        this.data.correspondence,
        partial(this.socket.send, 'outoftime')
      );
  }.bind(this);
  makeCorrespondenceClock();

  var correspondenceClockTick = function() {
    if (this.correspondenceClock && game.playable(this.data))
      this.correspondenceClock.tick(this.data.game.player);
  }.bind(this);

  if (this.clock) setInterval(clockTick, 100);
  else setInterval(correspondenceClockTick, 1000);

  var setQuietMode = function() {
    lichess.quietMode = game.isPlayerPlaying(this.data);
    document.body.classList.toggle('no-select',
      lichess.quietMode && this.clock && this.clock.secondsOf(this.data.player.color) <= 300);
  }.bind(this);
  setQuietMode();

  this.takebackYes = function() {
    this.socket.send('takeback-yes');
    this.chessground.cancelPremove();
  }.bind(this);

  this.moveOn = new moveOn(this, 'lichess.move_on');

  this.router = opts.routes;

  this.trans = function(key) {
    var str = opts.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  init(this);
};
