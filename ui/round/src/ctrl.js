var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var round = require('./round');
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
var sound = require('./sound');
var util = require('./util');

module.exports = function(opts) {

  this.data = round.merge({}, opts.data);

  this.userId = opts.userId;

  this.vm = {
    ply: init.startPly(this.data),
    initializing: true,
    flip: false,
    redirecting: false,
    replayHash: '',
    moveToSubmit: null,
    buttonFeedback: null,
    goneBerserk: {},
    resignConfirm: false,
    autoScroll: null
  };
  this.vm.goneBerserk[this.data.player.color] = opts.data.player.berserk;
  this.vm.goneBerserk[this.data.opponent.color] = opts.data.opponent.berserk;

  this.socket = new socket(opts.socketSend, this);

  var onUserMove = function(orig, dest, meta) {
    if (this.data.game.variant.key === 'standard')
      hold.register(this.socket, meta.holdTime);
    if (!promotion.start(this, orig, dest, meta.premove))
      this.sendMove(orig, dest, false, meta.premove);
  }.bind(this);

  var onMove = function(orig, dest, captured) {
    if (captured) {
      if (this.data.game.variant.key === 'atomic') {
        sound.explode();
        atomic.capture(this, dest, captured);
      } else sound.capture();
    } else sound.move();
  }.bind(this);

  this.chessground = ground.make(this.data, this.vm.ply, onUserMove, onMove);

  this.replaying = function() {
    return this.vm.ply !== round.lastPly(this.data);
  }.bind(this);

  this.stepsHash = function(steps) {
    var h = '';
    for (var i in steps) {
      h += steps[i].san;
    }
    return h;
  };

  this.jump = function(ply) {
    if (ply < round.firstPly(this.data) || ply > round.lastPly(this.data)) return;
    this.vm.ply = ply;
    var s = round.plyStep(this.data, ply);
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
    if (s.san) {
      if (s.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      if (/[+#]/.test(s.san)) sound.check();
    }
    this.vm.autoScroll && this.vm.autoScroll.throttle();
    return true;
  }.bind(this);

  this.replayEnabledByPref = function() {
    var d = this.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited' || d.game.speed === 'correspondence')
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

  this.sendMove = function(orig, dest, prom, isPremove) {
    var move = {
      from: orig,
      to: dest
    };
    if (prom) move.promotion = prom;
    if (blur.get()) move.b = 1;
    if (this.clock) move.lag = Math.round(lichess.socket.averageLag);
    this.resign(false);
    if (this.userId && this.data.pref.submitMove && !isPremove) {
      this.vm.moveToSubmit = move;
      m.redraw();
    } else this.socket.send('move', move, {
      ackable: true
    });
  }.bind(this);

  var showYourMoveNotification = function() {
    if (this.chessground.data.premovable.current)
    {
      var premove = this.chessground.data.premovable.current;
      var orig = premove[0], dest = premove[1];
      var canMove = chessground.board.canMove(this.chessground.data, orig, dest);
      if (chessground.board.canMove(this.chessground.data, orig, dest)) return;
    }
    if (game.isPlayerTurn(this.data)) lichess.desktopNotification(this.trans('yourTurn'));
  }.bind(this);

  this.apiMove = function(o) {
    m.startComputation();
    var d = this.data;
    d.game.turns = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'white' : 'black';
    var playedColor = o.ply % 2 === 0 ? 'black' : 'white';
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    d[d.player.color === 'white' ? 'player' : 'opponent'].offeringDraw = o.wDraw;
    d[d.player.color === 'black' ? 'player' : 'opponent'].offeringDraw = o.bDraw;
    d.possibleMoves = d.player.color === d.game.player ? o.dests : null;
    this.setTitle();
    if (!this.replaying()) {
      this.vm.ply++;
      this.chessground.apiMove(o.uci.substr(0, 2), o.uci.substr(2, 2));
      if (o.enpassant) {
        var p = o.enpassant,
          pieces = {};
        pieces[p.key] = null;
        this.chessground.setPieces(pieces);
        if (d.game.variant.key === 'atomic') {
          atomic.enpassant(this, p.key, p.color);
          sound.explode();
        } else sound.capture();
      }
      if (o.promotion) ground.promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      if (o.castle && !this.chessground.data.autoCastle) {
        var c = o.castle,
          pieces = {};
        pieces[c.king[0]] = null;
        pieces[c.rook[0]] = null;
        pieces[c.king[1]] = {
          role: 'king',
          color: c.color
        };
        pieces[c.rook[1]] = {
          role: 'rook',
          color: c.color
        };
        this.chessground.setPieces(pieces);
      }
      this.chessground.set({
        turnColor: d.game.player,
        movable: {
          dests: game.isPlayerPlaying(d) ? util.parsePossibleMoves(d.possibleMoves) : {}
        },
        check: o.check
      });
      if (o.check) $.sound.check();
      showYourMoveNotification();
    }
    if (o.clock) {
      var c = o.clock
      if (this.clock) this.clock.update(c.white, c.black);
      else if (this.correspondenceClock) this.correspondenceClock.update(c.white, c.black);
    }
    d.game.threefold = !!o.threefold;
    d.steps.push({
      ply: round.lastPly(this.data) + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check
    });
    game.setOnGame(d, playedColor, true);
    m.endComputation();
    if (d.blind) blind.reload(this);
    if (game.isPlayerPlaying(d) && playedColor === d.player.color) this.moveOn.next();

    if (!this.replaying() && playedColor !== d.player.color) {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      var premoveDelay = d.game.variant.key === 'atomic' ? 100 : 10;
      setTimeout(function() {
        if (!this.chessground.playPremove()) showYourMoveNotification();
      }.bind(this), premoveDelay);
    }
    this.vm.autoScroll && this.vm.autoScroll.now();
    onChange();
  }.bind(this);

  this.reload = function(cfg) {
    m.startComputation();
    if (this.stepsHash(cfg.steps) !== this.stepsHash(this.data.steps))
      this.vm.ply = cfg.steps[cfg.steps.length - 1].ply;
    this.data = round.merge(this.data, cfg);
    makeCorrespondenceClock();
    if (this.clock) this.clock.update(this.data.clock.white, this.data.clock.black);
    if (!this.replaying()) ground.reload(this.chessground, this.data, this.vm.ply, this.vm.flip);
    this.setTitle();
    if (this.data.blind) blind.reload(this);
    this.moveOn.next();
    setQuietMode();
    m.endComputation();
    this.vm.autoScroll && this.vm.autoScroll.now();
    onChange();
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

  this.resign = function(v) {
    if (this.vm.resignConfirm) {
      if (v) this.socket.send('resign');
      else this.vm.resignConfirm = false;
    } else if (v !== false) {
      if (this.data.pref.confirmResign) this.vm.resignConfirm = true;
      else this.socket.send('resign');
    }
  }.bind(this);

  this.goBerserk = function() {
    this.socket.berserk();
    $.sound.berserk();
  }.bind(this);

  this.setBerserk = function(color) {
    if (this.vm.goneBerserk[color]) return;
    this.vm.goneBerserk[color] = true;
    m.redraw();
  }.bind(this);

  this.moveOn = new moveOn(this, 'lichess.move_on');

  this.setRedirecting = function() {
    this.vm.redirecting = true;
    setTimeout(function() {
      this.vm.redirecting = false;
      m.redraw();
    }.bind(this), 2000);
  }.bind(this);

  this.submitMove = function(v) {
    if (v && this.vm.moveToSubmit) {
      this.socket.send('move', this.vm.moveToSubmit, {
        ackable: true
      });
      $.sound.confirmation();
    } else this.jump(this.vm.ply);
    this.vm.moveToSubmit = null;
    this.vm.buttonFeedback = setTimeout(function() {
      this.vm.buttonFeedback = null;
      m.redraw();
    }.bind(this), 500);
  }.bind(this);

  this.forecastInfo = function() {
    return game.forecastable(this.data) &&
      !this.replaying() &&
      this.data.game.turns > 1 &&
      lichess.once('forecast-info-seen5');
  }.bind(this);

  var onChange = function() {
    opts.onChange && setTimeout(partial(opts.onChange,this.data), 200);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  init.yolo(this);

  onChange();
};
