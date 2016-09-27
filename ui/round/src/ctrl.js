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
var xhr = require('./xhr');
var crazyValid = require('./crazy/crazyValid');
var keyboardMove = require('./keyboardMove');
var renderUser = require('./view/user');

module.exports = function(opts) {

  this.data = round.merge({}, opts.data).data;

  this.userId = opts.userId;

  this.vm = {
    ply: init.startPly(this.data),
    initializing: true,
    firstSeconds: true,
    flip: false,
    loading: false,
    loadingTimeout: null,
    redirecting: false,
    replayHash: '',
    moveToSubmit: null,
    dropToSubmit: null,
    goneBerserk: {},
    resignConfirm: false,
    autoScroll: null,
    element: opts.element,
    challengeRematched: false,
    justDropped: null
  };
  this.vm.goneBerserk[this.data.player.color] = opts.data.player.berserk;
  this.vm.goneBerserk[this.data.opponent.color] = opts.data.opponent.berserk;
  setTimeout(function() {
    this.vm.firstSeconds = false;
    m.redraw();
  }.bind(this), 3000);

  this.socket = new socket(opts.socketSend, this);

  var onUserMove = function(orig, dest, meta) {
    if (hold.applies(this.data)) {
      hold.register(this, meta);
      if (this.vm.ply > 12 && this.vm.ply <= 14) hold.find(this);
    }
    if (!promotion.start(this, orig, dest, meta.premove))
      this.sendMove(orig, dest, false, meta.premove);
  }.bind(this);

  var onUserNewPiece = function(role, key, meta) {
    if (!this.replaying() && crazyValid.drop(this.chessground, this.data, role, key))
      this.sendNewPiece(role, key, meta.predrop);
    else this.jump(this.vm.ply);
  }.bind(this);

  var onMove = function(orig, dest, captured) {
    if (captured) {
      if (this.data.game.variant.key === 'atomic') {
        sound.explode();
        atomic.capture(this, dest, captured);
      } else sound.capture();
    } else sound.move();
  }.bind(this);

  var onNewPiece = function(piece, key) {
    sound.move();
  }.bind(this);

  this.chessground = ground.make({
    data: this.data,
    ply: this.vm.ply,
    onUserMove: onUserMove,
    onUserNewPiece: onUserNewPiece,
    onMove: onMove,
    onNewPiece: onNewPiece
  });

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

  var uciToLastMove = function(uci) {
    if (!uci) return;
    if (uci[1] === '@') return [uci.substr(2, 2), uci.substr(2, 2)];
    return [uci.substr(0, 2), uci.substr(2, 2)];
  };

  this.userJump = function(ply) {
    this.cancelMove();
    this.chessground.selectSquare(null);
    this.jump(ply);
  }.bind(this);

  this.jump = function(ply) {
    if (ply < round.firstPly(this.data) || ply > round.lastPly(this.data)) return;
    this.vm.ply = ply;
    this.vm.justDropped = null;
    var s = round.plyStep(this.data, ply);
    var config = {
      fen: s.fen,
      lastMove: uciToLastMove(s.uci),
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
      orientation: ground.boardOrientation(this.data, this.vm.flip)
    });
    m.redraw();
  }.bind(this);

  this.setTitle = partial(title.set, this);

  this.sendMove = function(orig, dest, prom, isPremove) {
    var move = {
      u: orig + dest
    };
    if (prom) move.u += (prom === 'knight' ? 'n' : prom[0]);
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.userId && this.data.pref.submitMove && !isPremove) {
      this.vm.moveToSubmit = move;
      m.redraw();
    } else this.socket.send('move', move, {
      ackable: true,
      withLag: !!this.clock
    });
    this.vm.justDropped = null;
  }.bind(this);

  this.sendNewPiece = function(role, key, isPredrop) {
    var drop = {
      role: role,
      pos: key
    };
    this.resign(false);
    if (this.userId && this.data.pref.submitMove && !isPredrop) {
      this.vm.dropToSubmit = drop;
      m.redraw();
    } else this.socket.send('drop', drop, {
      ackable: true,
      withLag: !!this.clock
    });
    this.vm.justDropped = {
      ply: this.vm.ply,
      role: role
    };
    m.redraw();
  }.bind(this);

  var showYourMoveNotification = function() {
    if (game.isPlayerTurn(this.data)) lichess.desktopNotification(function() {
      var txt = this.trans('yourTurn');
      var opponent = renderUser.userTxt(this, this.data.opponent);
      if (this.vm.ply < 2)
        txt = opponent + '\njoined the game.\n' + txt;
      else {
        var move = this.data.steps[this.data.steps.length - 2].san;
        var turn = Math.floor((this.vm.ply - 1) / 2) + 1;
        move = turn + (this.vm.ply % 2 === 1 ? '.' : '...') + ' ' + move;
        txt = opponent + '\nplayed ' + move + '.\n' + txt;
      }
      return txt;
    }.bind(this));
  }.bind(this);
  setTimeout(showYourMoveNotification, 500);

  this.apiMove = function(o) {
    m.startComputation();
    var d = this.data,
      playing = game.isPlayerPlaying(d);
    d.game.turns = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'white' : 'black';
    var playedColor = o.ply % 2 === 0 ? 'black' : 'white';
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    d[d.player.color === 'white' ? 'player' : 'opponent'].offeringDraw = o.wDraw;
    d[d.player.color === 'black' ? 'player' : 'opponent'].offeringDraw = o.bDraw;
    d.possibleMoves = d.player.color === d.game.player ? o.dests : null;
    d.possibleDrops = d.player.color === d.game.player ? o.drops : null;
    d.crazyhouse = o.crazyhouse;
    this.setTitle();
    if (!this.replaying()) {
      this.vm.ply++;
      if (o.isMove) this.chessground.apiMove(o.uci.substr(0, 2), o.uci.substr(2, 2));
      else this.chessground.apiNewPiece({
        role: o.role,
        color: playedColor
      }, o.uci.substr(2, 2));
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
          dests: playing ? util.parsePossibleMoves(d.possibleMoves) : {}
        },
        check: o.check
      });
      if (o.check) $.sound.check();
    }
    if (o.clock)(this.clock || this.correspondenceClock).update(o.clock.white, o.clock.black);
    d.game.threefold = !!o.threefold;
    d.steps.push({
      ply: round.lastPly(this.data) + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check,
      crazy: o.crazyhouse
    });
    game.setOnGame(d, playedColor, true);
    delete this.data.forecastCount;
    m.endComputation();
    if (d.blind) blind.reload(this);
    if (playing && playedColor === d.player.color) this.moveOn.next();

    if (!this.replaying() && playedColor !== d.player.color) {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      var premoveDelay = d.game.variant.key === 'atomic' ? 100 : 10;
      setTimeout(function() {
        if (!this.chessground.playPremove() && !playPredrop()) showYourMoveNotification();
      }.bind(this), premoveDelay);
    }
    this.vm.autoScroll && this.vm.autoScroll.now();
    onChange();
    if (this.music) this.music.jump(o);
  }.bind(this);

  var playPredrop = function() {
    return this.chessground.playPredrop(function(drop) {
      return crazyValid.drop(this.chessground, this.data, drop.role, drop.key);
    }.bind(this));
  }.bind(this);

  this.reload = function(cfg) {
    m.startComputation();
    if (this.stepsHash(cfg.steps) !== this.stepsHash(this.data.steps))
      this.vm.ply = cfg.steps[cfg.steps.length - 1].ply;
    var merged = round.merge(this.data, cfg);
    this.data = merged.data;
    this.vm.justDropped = null;
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
    this.setLoading(false);
    if (merged.changes.drawOffer) lichess.desktopNotification(this.trans('yourOpponentOffersADraw'));
    if (merged.changes.takebackOffer) lichess.desktopNotification(this.trans('yourOpponentProposesATakeback'));
    if (merged.changes.rematchOffer) lichess.desktopNotification(this.trans('yourOpponentWantsToPlayANewGameWithYou'));
  }.bind(this);

  this.challengeRematch = function() {
    this.vm.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(function() {
      lichess.challengeApp.open();
      if (lichess.once('rematch-challenge')) setTimeout(function() {
        lichess.hopscotch(function() {
          hopscotch.configure({
            i18n: {
              doneBtn: 'OK, got it'
            }
          }).startTour({
            id: "rematch-challenge",
            showPrevButton: true,
            steps: [{
              title: "Challenged to a rematch",
              content: 'Your opponent is offline, but they can accept this challenge later!',
              target: "#challenge_app",
              placement: "bottom"
            }]
          });
        });
      }, 1000);
    }, function(data) {
      this.vm.challengeRematched = false;
      $.modal(data.error);
    }.bind(this));
  }.bind(this);

  this.clock = this.data.clock ? clockCtrl(
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

  if (this.clock) {
    var tickNow = function() {
      clockTick();
      if (game.playable(this.data)) setTimeout(tickNow, 100);
    }.bind(this);
    setTimeout(tickNow, 100);
  } else setInterval(correspondenceClockTick, 1000);

  var setQuietMode = function() {
    lichess.quietMode = game.isPlayerPlaying(this.data);
    document.body.classList.toggle('no-select',
      lichess.quietMode && this.clock && this.clock.secondsOf(this.data.player.color) <= 300);
  }.bind(this);
  setQuietMode();

  this.takebackYes = function() {
    this.socket.sendLoading('takeback-yes');
    this.chessground.cancelPremove();
  }.bind(this);

  this.resign = function(v) {
    if (this.vm.resignConfirm) {
      if (v) this.socket.sendLoading('resign');
      else this.vm.resignConfirm = false;
    } else if (v !== false) {
      if (this.data.pref.confirmResign) this.vm.resignConfirm = true;
      else this.socket.sendLoading('resign');
    }
  }.bind(this);

  this.goBerserk = function() {
    this.socket.berserk();
    $.sound.berserk();
  }.bind(this);

  this.setBerserk = function(color) {
    if (this.vm.goneBerserk[color]) return;
    this.vm.goneBerserk[color] = true;
    if (color !== this.data.player.color) $.sound.berserk();
    m.redraw();
  }.bind(this);

  this.moveOn = new moveOn(this, 'lichess.move_on');

  this.setLoading = function(v) {
    clearTimeout(this.vm.loadingTimeout);
    if (v) {
      this.vm.loading = true;
      this.vm.loadingTimeout = setTimeout(function() {
        this.vm.loading = false;
        m.redraw();
      }.bind(this), 1000);
    } else {
      this.vm.loading = false;
    }
    m.redraw();
  }.bind(this);

  this.setRedirecting = function() {
    this.vm.redirecting = true;
    setTimeout(function() {
      this.vm.redirecting = false;
      m.redraw();
    }.bind(this), 2500);
    m.redraw();
  }.bind(this);

  this.submitMove = function(v) {
    if (v && (this.vm.moveToSubmit || this.vm.dropToSubmit)) {
      if (this.vm.moveToSubmit)
        this.socket.send('move', this.vm.moveToSubmit, {
          ackable: true
        });
      else if (this.vm.dropToSubmit)
        this.socket.send('drop', this.vm.dropToSubmit, {
          ackable: true
        });
      $.sound.confirmation();
    } else this.jump(this.vm.ply);
    this.cancelMove();
    this.setLoading(true);
  }.bind(this);

  this.cancelMove = function(v) {
    this.vm.moveToSubmit = null;
    this.vm.dropToSubmit = null;
  }.bind(this);

  var forecastable = function(d) {
    return game.isPlayerPlaying(d) && d.correspondence && !d.opponent.ai;
  }

  this.forecastInfo = function() {
    return forecastable(this.data) &&
      !this.replaying() &&
      this.data.game.turns > 1 &&
      lichess.once('forecast-info-seen6');
  }.bind(this);

  var onChange = function() {
    opts.onChange && setTimeout(partial(opts.onChange, this.data), 200);
  }.bind(this);

  this.forceResignable = function() {
    return !this.data.opponent.ai && this.data.clock && this.data.opponent.isGone && game.resignable(this.data);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  this.keyboardMove = this.data.pref.keyboardMove ? keyboardMove.ctrl(this) : null;

  init.yolo(this);

  onChange();

  this.music = null;
  lichess.pubsub.on('sound_set', function(set) {
    if (!this.music && set === 'music')
      lichess.loadScript('/assets/javascripts/music/play.js').then(function() {
        this.music = lichessPlayMusic();
      }.bind(this));
    if (this.music && set !== 'music') this.music = null;
  }.bind(this));
};
