var round = require('./round');
var game = require('game').game;
var status = require('game').status;
var ground = require('./ground');
var socket = require('./socket');
var title = require('./title');
var promotion = require('./promotion');
var blur = require('./blur');
var blind = require('./blind');
var clockCtrl = require('./clock/ctrl');
var correspondenceClockCtrl = require('./correspondenceClock/ctrl');
var moveOn = require('./moveOn');
var atomic = require('./atomic');
var sound = require('./sound');
var util = require('./util');
var xhr = require('./xhr');
var crazyValid = require('./crazy/crazyCtrl').valid;
var makeKeyboardMove = require('./keyboardMove').ctrl;
var renderUser = require('./view/user');
var cevalSub = require('./cevalSub');
var keyboard = require('./keyboard');

var nowMillis = Performance.now || Date.now;

module.exports = function(opts, redraw) {

  this.data = round.merge({}, opts.data).data;

  this.userId = opts.userId;
  this.opts = opts;
  this.redraw = redraw;

  this.vm = {
    ply: round.lastPly(this.data),
    firstSeconds: true,
    flip: false,
    loading: false,
    loadingTimeout: null,
    redirecting: false,
    moveToSubmit: null,
    dropToSubmit: null,
    goneBerserk: {},
    resignConfirm: false,
    autoScroll: null,
    challengeRematched: false,
    justDropped: null,
    justCaptured: null,
    preDrop: null,
    lastDrawOfferAtPly: null,
    lastMoveMillis: null
  };
  this.vm.goneBerserk[this.data.player.color] = opts.data.player.berserk;
  this.vm.goneBerserk[this.data.opponent.color] = opts.data.opponent.berserk;
  setTimeout(function() {
    this.vm.firstSeconds = false;
    redraw();
  }.bind(this), 3000);

  this.socket = new socket(opts.socket, this);

  var onUserMove = function(orig, dest, meta) {
    lichess.ab && (!this.keyboardMove || !this.keyboardMove.usedSan) && lichess.ab(this, meta);
    if (!promotion.start(this, orig, dest, meta)) this.sendMove(orig, dest, false, meta);
  }.bind(this);

  var onUserNewPiece = function(role, key, meta) {
    if (!this.replaying() && crazyValid(this.data, role, key)) {
      this.sendNewPiece(role, key, meta.predrop);
    } else this.jump(this.vm.ply);
  }.bind(this);

  var onMove = function(orig, dest, captured) {
    if (captured) {
      if (this.data.game.variant.key === 'atomic') {
        sound.explode();
        atomic.capture(this, dest, captured);
      } else sound.capture();
    } else sound.move();
  }.bind(this);

  var onPremove = function(orig, dest, meta) {
    promotion.start(this, orig, dest, meta);
  }.bind(this);

  var onCancelPremove = function() {
    promotion.cancelPrePromotion(this);
  }.bind(this);

  var onPredrop = function(role) {
    this.vm.preDrop = role;
    redraw();
  }.bind(this);

  var onNewPiece = function(piece, key) {
    sound.move();
  }.bind(this);

  this.makeCgHooks = function() {
    return {
      onUserMove: onUserMove,
      onUserNewPiece: onUserNewPiece,
      onMove: onMove,
      onNewPiece: onNewPiece,
      onPremove: onPremove,
      onCancelPremove: onCancelPremove,
      onPredrop: onPredrop
    };
  };

  this.replaying = function() {
    return this.vm.ply !== round.lastPly(this.data);
  }.bind(this);

  this.userJump = function(ply) {
    this.cancelMove();
    this.chessground.selectSquare(null);
    this.jump(ply);
  }.bind(this);

  this.jump = function(ply) {
    if (ply < round.firstPly(this.data) || ply > round.lastPly(this.data)) return;
    this.vm.ply = ply;
    this.vm.justDropped = null;
    this.vm.preDrop = null;
    var s = round.plyStep(this.data, ply);
    var config = {
      fen: s.fen,
      lastMove: util.uci2move(s.uci),
      check: !!s.check,
      turnColor: this.vm.ply % 2 === 0 ? 'white' : 'black'
    };
    if (this.replaying()) this.chessground.stop();
    else config.movable = {
      color: game.isPlayerPlaying(this.data) && this.data.player.color,
      dests: util.parsePossibleMoves(this.data.possibleMoves)
    }
    this.chessground.set(config);
    if (s.san) {
      if (s.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      if (/[+#]/.test(s.san)) sound.check();
    }
    this.vm.autoScroll && this.vm.autoScroll();
    if (this.keyboardMove) this.keyboardMove.update(s);
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
    redraw();
  }.bind(this);

  this.setTitle = () => title.set(this);

  this.actualSendMove = function(type, action, meta) {
    meta = meta === undefined ? {} : meta
    var socketOpts = {
      ackable: true,
      // withLag: !!this.clock
    }
    var startTime = this.vm.lastMoveMillis;
    if (startTime !== null) socketOpts.millis = nowMillis() - startTime;
    this.socket.send(type, action, socketOpts);

    this.vm.justDropped = meta.justDropped;
    this.vm.justCaptured = meta.justCaptured;
    this.vm.preDrop = null;
    redraw();
  }

  this.sendMove = function(orig, dest, prom, meta) {
    var move = {
      u: orig + dest
    };
    if (prom) move.u += (prom === 'knight' ? 'n' : prom[0]);
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premove) {
      this.vm.moveToSubmit = move;
      redraw();
    } else this.actualSendMove('move', move, {justCaptured: meta.captured});

  }.bind(this);

  this.sendNewPiece = function(role, key, isPredrop) {
    var drop = {
      role: role,
      pos: key
    };
    if (blur.get()) drop.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !isPredrop) {
      this.vm.dropToSubmit = drop;
      redraw();
    } else {
      this.actualSendMove('drop', drop, {justDropped: role});
    }
  }.bind(this);

  var showYourMoveNotification = function() {
    var d = this.data;
    if (game.isPlayerTurn(d)) lichess.desktopNotification(function() {
      var txt = this.trans('yourTurn');
      var opponent = renderUser.userTxt(this, d.opponent);
      if (this.vm.ply < 1)
      txt = opponent + '\njoined the game.\n' + txt;
      else {
        var move = d.steps[d.steps.length - 1].san;
        var turn = Math.floor((this.vm.ply - 1) / 2) + 1;
        move = turn + (this.vm.ply % 2 === 1 ? '.' : '...') + ' ' + move;
        txt = opponent + '\nplayed ' + move + '.\n' + txt;
      }
      return txt;
    }.bind(this));
    else if (game.isPlayerPlaying(d) && this.vm.ply < 1) lichess.desktopNotification(function() {
      return renderUser.userTxt(this, d.opponent) + '\njoined the game.';
    }.bind(this));
  }.bind(this);
  setTimeout(showYourMoveNotification, 500);

  this.apiMove = function(o) {
    var d = this.data,
      playing = game.isPlayerPlaying(d);

    if (playing) this.vm.lastMoveMillis = nowMillis();
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
      if (o.isMove) {
        var keys = util.uci2move(o.uci);
        this.chessground.move(keys[0], keys[1]);
      }
      else this.chessground.newPiece({
        role: o.role,
        color: playedColor
      }, o.uci.substr(2, 2));
      if (o.enpassant) {
        var p = o.enpassant,
          pieces = {};
        pieces[p.key] = false;
        this.chessground.setPieces(pieces);
        if (d.game.variant.key === 'atomic') {
          atomic.enpassant(this, p.key, p.color);
          sound.explode();
        } else sound.capture();
      }
      if (o.promotion) ground.promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      if (o.castle && !this.chessground.state.autoCastle) {
        var c = o.castle,
          pieces = {};
        pieces[c.king[0]] = false;
        pieces[c.rook[0]] = false;
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
        check: !!o.check
      });
      if (o.check) sound.check();
      blur.onMove();
    }
    if (o.clock)(this.clock || this.correspondenceClock).update(o.clock.white, o.clock.black);
    d.game.threefold = !!o.threefold;
    var step = {
      ply: round.lastPly(this.data) + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check,
      crazy: o.crazyhouse
    };
    d.steps.push(step);
    this.vm.justDropped = null;
    this.vm.justCaptured = null;
    game.setOnGame(d, playedColor, true);
    delete this.data.forecastCount;
    redraw();
    if (d.blind) blind.reload(this);
    if (playing && playedColor === d.player.color) {
      this.moveOn.next();
      cevalSub.publish(this, o);
    }
    if (!this.replaying() && playedColor !== d.player.color) {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      var premoveDelay = d.game.variant.key === 'atomic' ? 100 : 1;
      setTimeout(function() {
        if (!this.chessground.playPremove() && !playPredrop()) {
          promotion.cancel(this);
          showYourMoveNotification();
        }
      }.bind(this), premoveDelay);
    }
    this.vm.autoScroll && this.vm.autoScroll();
    onChange();
    if (this.keyboardMove) this.keyboardMove.update(step);
    if (this.music) this.music.jump(o);
  }.bind(this);

  var playPredrop = function() {
    return this.chessground.playPredrop(function(drop) {
      return crazyValid(this.data, drop.role, drop.key);
    }.bind(this));
  }.bind(this);

  this.reload = function(cfg) {
    if (cfg.steps.length !== this.data.steps.length) this.vm.ply = cfg.steps[cfg.steps.length - 1].ply;
    var merged = round.merge(this.data, cfg);
    this.data = merged.data;
    this.vm.justDropped = null;
    this.vm.justCaptured = null;
    this.vm.preDrop = null;
    makeCorrespondenceClock();
    if (this.clock) this.clock.update(this.data.clock.white, this.data.clock.black);
    if (this.correspondenceClock) this.correspondenceClock.update(this.data.correspondence.white, this.data.correspondence.black);
    if (!this.replaying()) ground.reload(this);
    this.setTitle();
    if (this.data.blind) blind.reload(this);
    this.moveOn.next();
    setQuietMode();
    redraw();
    this.vm.autoScroll && this.vm.autoScroll();
    onChange();
    this.setLoading(false);
    if (merged.changes.drawOffer) lichess.desktopNotification(this.trans('yourOpponentOffersADraw'));
    if (merged.changes.takebackOffer) lichess.desktopNotification(this.trans('yourOpponentProposesATakeback'));
    if (merged.changes.rematchOffer) lichess.desktopNotification(this.trans('yourOpponentWantsToPlayANewGameWithYou'));
    if (this.keyboardMove) this.keyboardMove.update(cfg.steps[cfg.steps.length - 1]);
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

  this.clock = this.data.clock ? clockCtrl(this.data.clock, {
    onFlag: function() {
      this.socket.outoftime();
      redraw();
    }.bind(this),
    soundColor: (this.data.simul || this.data.player.spectator || !this.data.pref.clockSound) ? null : this.data.player.color
  }) : false;

  this.isClockRunning = function() {
    return this.data.clock && game.playable(this.data) &&
    ((this.data.game.turns - this.data.game.startedAtTurn) > 1 || this.data.clock.running);
  }.bind(this);

  var clockTick = function() {
    if (this.isClockRunning()) this.clock.tick(this, this.data.game.player);
  }.bind(this);

  var makeCorrespondenceClock = function() {
    if (this.data.correspondence && !this.correspondenceClock)
    this.correspondenceClock = new correspondenceClockCtrl(
      this.data.correspondence,
      () => this.socket.send('outoftime')
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
      lichess.quietMode && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5);
  }.bind(this);
  setQuietMode();

  this.takebackYes = function() {
    this.socket.sendLoading('takeback-yes');
    this.chessground.cancelPremove();
    promotion.cancel(this);
  }.bind(this);

  this.resign = function(v) {
    if (this.vm.resignConfirm) {
      if (v) this.socket.sendLoading('resign');
      else this.vm.resignConfirm = false;
    } else if (v !== false) {
      if (this.data.pref.confirmResign) this.vm.resignConfirm = true;
      else this.socket.sendLoading('resign');
    }
    redraw();
  }.bind(this);

  this.goBerserk = function() {
    this.socket.berserk();
    lichess.sound.berserk();
  }.bind(this);

  this.setBerserk = function(color) {
    if (this.vm.goneBerserk[color]) return;
    this.vm.goneBerserk[color] = true;
    if (color !== this.data.player.color) lichess.sound.berserk();
    redraw();
  }.bind(this);

  this.moveOn = new moveOn(this, 'lichess.move_on');

  this.setLoading = function(v, duration) {
    clearTimeout(this.vm.loadingTimeout);
    if (v) {
      this.vm.loading = true;
      this.vm.loadingTimeout = setTimeout(function() {
        this.vm.loading = false;
        redraw();
      }.bind(this), duration || 1500);
    } else {
      this.vm.loading = false;
    }
    redraw();
  }.bind(this);

  this.setRedirecting = function() {
    this.vm.redirecting = true;
    setTimeout(function() {
      this.vm.redirecting = false;
      redraw();
    }.bind(this), 2500);
    redraw();
  }.bind(this);

  this.submitMove = function(v) {
    if (v && (this.vm.moveToSubmit || this.vm.dropToSubmit)) {
      if (this.vm.moveToSubmit) {
        this.actualSendMove('move', this.vm.moveToSubmit);
      } else {
        this.actualSendMove('drop', this.vm.dropToSubmit);
      }
      lichess.sound.confirmation();
    } else this.jump(this.vm.ply);
    this.cancelMove();
    this.setLoading(true, 300);
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
    opts.onChange && setTimeout(() => opts.onChange(this.data), 200);
  }.bind(this);

  this.forceResignable = function() {
    return !this.data.opponent.ai && this.data.clock && this.data.opponent.isGone && game.resignable(this.data);
  }.bind(this);

  this.canOfferDraw = function() {
    return game.drawable(this.data) && (this.lastDrawOfferAtPly || -99) < (this.vm.ply - 20);
  }.bind(this);

  this.offerDraw = function() {
    if (this.canOfferDraw()) {
      this.lastDrawOfferAtPly = this.vm.ply;
      this.socket.sendLoading('draw-yes', null)
    }
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  this.setChessground = function(cg) {
    this.chessground = cg;
    if (this.data.pref.keyboardMove) {
      this.keyboardMove = makeKeyboardMove(cg, round.plyStep(this.data, this.vm.ply), redraw);
    }
  }.bind(this);

  setTimeout(function delayedInit() {
    if (game.isPlayerPlaying(this.data) &&
        game.nbMoves(this.data, this.data.player.color) === 0) {
      lichess.sound.genericNotify();
    }
    lichess.requestIdleCallback(function idleCallback() {
      if (game.isPlayerPlaying(this.data)) {
        if (!this.data.simul) blur.init();

        title.init(this);
        this.setTitle();

        window.addEventListener('beforeunload', function(e) {
          if (!lichess.hasToReload && !this.data.blind && game.playable(this.data) && this.data.clock && !this.data.opponent.ai) {
            document.body.classList.remove('fpmenu');
            this.socket.send('bye2');
            var msg = 'There is a game in progress!';
            (e || window.event).returnValue = msg;
            return msg;
          }
        }.bind(this));

        Mousetrap.bind(['esc'], function() {
          this.chessground.cancelMove();
        }.bind(this));

        cevalSub.subscribe(this);
      }

      keyboard.init(this);

      onChange();

    }.bind(this));
  }.bind(this), 200);

  lichess.pubsub.on('jump', function(ply) {
    this.jump(parseInt(ply));
    redraw();
  }.bind(this));

  this.music = null;
  lichess.pubsub.on('sound_set', function(set) {
    if (!this.music && set === 'music')
      lichess.loadScript('/assets/javascripts/music/play.js').then(function() {
        this.music = lichessPlayMusic();
      }.bind(this));
      if (this.music && set !== 'music') this.music = null;
  }.bind(this));
};
