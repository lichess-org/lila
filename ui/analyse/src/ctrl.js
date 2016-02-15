var chessground = require('chessground');
var opposite = chessground.util.opposite;
var data = require('./data');
var analyse = require('./analyse');
var treePath = require('./path');
var ground = require('./ground');
var keyboard = require('./keyboard');
var actionMenu = require('./actionMenu').controller;
var autoplay = require('./autoplay');
var control = require('./control');
var promotion = require('./promotion');
var util = require('./util');
var throttle = require('./util').throttle;
var socket = require('./socket');
var forecastCtrl = require('./forecast/forecastCtrl');
var cevalCtrl = require('./ceval/cevalCtrl');
var explorerCtrl = require('./explorer/explorerCtrl');
var router = require('game').router;
var game = require('game').game;
var crazyValid = require('./crazy/crazyValid');
var m = require('mithril');

module.exports = function(opts) {

  this.data = data({}, opts.data);
  this.userId = opts.userId;
  this.ongoing = !util.synthetic(this.data) && game.playable(this.data);
  this.onMyTurn = this.data

  this.analyse = new analyse(this.data.steps);
  this.actionMenu = new actionMenu();
  this.autoplay = new autoplay(this);

  var initialPath = opts.path ? (opts.path === 'last' ? treePath.default(this.analyse.lastPly()) : treePath.read(opts.path)) : treePath.default(this.analyse.firstPly());
  if (initialPath[0].ply >= this.data.steps.length)
    initialPath = treePath.default(this.data.steps.length - 1);

  this.vm = {
    path: initialPath,
    pathStr: treePath.write(initialPath),
    initialPathStr: '' + opts.data.path,
    step: null,
    cgConfig: null,
    comments: true,
    flip: false,
    showAutoShapes: util.storedProp('show-auto-shapes', true),
    showGauge: util.storedProp('show-gauge', true),
    autoScroll: null,
    variationMenu: null,
    element: opts.element
  };

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.vm.flip ? this.data.opponent.color : this.data.player.color
    });
  }.bind(this);

  this.togglePlay = function(delay) {
    this.autoplay.toggle(delay);
    this.actionMenu.open = false;
  }.bind(this);

  var uciToLastMove = function(uci) {
    if (!uci) return;
    if (uci[1] === '@') return [uci.substr(2, 2), uci.substr(2, 2)];
    return [uci.substr(0, 2), uci.substr(2, 2)];
  };

  var showGround = function() {
    var s;
    try {
      s = this.analyse.getStep(this.vm.path);
    } catch (e) {
      console.log(e);
    }
    if (!s) {
      this.vm.path = treePath.default(this.analyse.firstPly());
      this.vm.pathStr = treePath.write(this.vm.path);
      s = this.analyse.getStep(this.vm.path);
    }
    var color = s.ply % 2 === 0 ? 'white' : 'black';
    var dests = util.readDests(s.dests);
    var drops = util.readDrops(s.drops);
    var config = {
      fen: s.fen,
      turnColor: color,
      movable: {
        color: (dests && Object.keys(dests).length > 0) || drops === null || drops.length ? color : null,
        dests: dests || {}
      },
      check: s.check,
      lastMove: uciToLastMove(s.uci)
    };
    if (!dests && !s.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable.color = color;
    }
    this.vm.step = s;
    this.vm.cgConfig = config;
    if (!this.chessground)
      this.chessground = ground.make(this.data, config, userMove, userNewPiece);
    this.chessground.set(config);
    onChange();
    if (!dests) getDests();
    this.setAutoShapes();
  }.bind(this);

  var getDests = throttle(800, false, function() {
    if (this.vm.step.dests) return;
    this.socket.sendAnaDests({
      variant: this.data.game.variant.key,
      fen: this.vm.step.fen,
      path: this.vm.pathStr
    });
  }.bind(this));

  var sound = {
    move: throttle(50, false, $.sound.move),
    capture: throttle(50, false, $.sound.capture),
    check: throttle(50, false, $.sound.check)
  };

  var onChange = opts.onChange ? throttle(500, false, function() {
    opts.onChange(this.vm.step.fen, this.vm.path);
  }.bind(this)) : $.noop;

  var updateHref = window.history.replaceState ? throttle(750, false, function() {
    window.history.replaceState(null, null, '#' + this.vm.path[0].ply);
  }.bind(this), false) : $.noop;

  this.jump = function(path) {
    this.vm.path = path;
    this.vm.pathStr = treePath.write(path);
    this.toggleVariationMenu(null);
    showGround();
    if (!this.vm.step.uci) sound.move(); // initial position
    else if (this.vm.justPlayed !== this.vm.step.uci) {
      if (this.vm.step.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      this.vm.justPlayed = null;
    }
    if (/\+|\#/.test(this.vm.step.san)) sound.check();
    this.ceval.stop();
    startCeval();
    this.explorer.setStep();
    updateHref();
    this.vm.autoScroll && this.vm.autoScroll();
    promotion.cancel(this);
  }.bind(this);

  this.userJump = function(path) {
    this.autoplay.stop();
    this.jump(path);
  }.bind(this);

  this.jumpToMain = function(ply) {
    this.userJump([{
      ply: ply,
      variation: null
    }]);
  }.bind(this);

  this.jumpToIndex = function(index) {
    this.jumpToMain(index + 1 + this.data.game.startedAtTurn);
  }.bind(this);

  this.jumpToNag = function(color, nag) {
    var ply = this.analyse.plyOfNextNag(color, nag, this.vm.step.ply);
    if (ply) this.jumpToMain(ply);
    m.redraw();
  }.bind(this);

  var forsyth = function(role) {
    return role === 'knight' ? 'n' : role[0];
  };

  var roleToSan = {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q'
  };
  var sanToRole = {
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen'
  };

  var userNewPiece = function(piece, pos) {
    if (crazyValid.drop(this.chessground, this.vm.step.drops, piece, pos)) {
      this.vm.justPlayed = roleToSan[piece.role] + '@' + pos;
      sound.move();
      var drop = {
        role: piece.role,
        pos: pos,
        variant: this.data.game.variant.key,
        fen: this.vm.step.fen,
        path: this.vm.pathStr
      };
      this.socket.sendAnaDrop(drop);
      preparePremoving();
    } else this.jump(this.vm.path);
  }.bind(this);

  var userMove = function(orig, dest, capture) {
    this.vm.justPlayed = orig + dest;
    sound[capture ? 'capture' : 'move']();
    if (!promotion.start(this, orig, dest, sendMove)) sendMove(orig, dest);
  }.bind(this);

  var sendMove = function(orig, dest, prom) {
    var move = {
      orig: orig,
      dest: dest,
      variant: this.data.game.variant.key,
      fen: this.vm.step.fen,
      path: this.vm.pathStr
    };
    if (prom) move.promotion = prom;
    this.socket.sendAnaMove(move);
    preparePremoving();
  }.bind(this);

  var preparePremoving = function() {
    this.chessground.set({
      turnColor: this.chessground.data.movable.color,
      movable: {
        color: opposite(this.chessground.data.movable.color)
      }
    });
  }.bind(this);

  this.addStep = function(step, path) {
    var newPath = this.analyse.addStep(step, treePath.read(path));
    this.jump(newPath);
    m.redraw();
    this.chessground.playPremove();
  }.bind(this);

  this.addDests = function(dests, path) {
    this.analyse.addDests(dests, treePath.read(path));
    if (path === this.vm.pathStr) {
      showGround();
      m.redraw();
      if (dests === '') this.ceval.stop();
    }
    this.chessground.playPremove();
  }.bind(this);

  this.toggleVariationMenu = function(path) {
    if (!path) this.vm.variationMenu = null;
    else {
      var key = treePath.write(path.slice(0, 1));
      this.vm.variationMenu = this.vm.variationMenu === key ? null : key;
    }
  }.bind(this);

  this.deleteVariation = function(path) {
    var ply = path[0].ply;
    var id = path[0].variation;
    this.analyse.deleteVariation(ply, id);
    if (treePath.contains(path, this.vm.path)) this.jumpToMain(ply - 1);
    this.toggleVariationMenu(null);
  }.bind(this);

  this.promoteVariation = function(path) {
    var ply = path[0].ply;
    var id = path[0].variation;
    this.analyse.promoteVariation(ply, id);
    if (treePath.contains(path, this.vm.path))
      this.jump(this.vm.path.splice(1));
    this.toggleVariationMenu(null);
  }.bind(this);

  this.reset = function() {
    showGround();
    m.redraw();
  }.bind(this);

  this.encodeStepFen = function() {
    return this.vm.step.fen.replace(/\s/g, '_');
  }.bind(this);

  this.socket = new socket(opts.socketSend, this);

  this.currentAnyEval = function() {
    return this.vm.step ? (this.vm.step.eval || this.vm.step.ceval) : null;
  }.bind(this);

  this.forecast = opts.data.forecast ? forecastCtrl(
    opts.data.forecast,
    router.forecasts(this.data)) : null;

  var allowCeval = (
    util.synthetic(this.data) || !game.playable(this.data)
  ) && ['standard', 'fromPosition', 'chess960'].indexOf(this.data.game.variant.key) !== -1;

  this.ceval = cevalCtrl(allowCeval, function(res) {
    this.analyse.updateAtPath(res.work.path, function(step) {
      if (step.ceval && step.ceval.depth >= res.eval.depth) return;
      step.ceval = res.eval;
      if (treePath.write(res.work.path) === this.vm.pathStr) {
        this.setAutoShapes();
        m.redraw();
      }
    }.bind(this));
  }.bind(this));

  var canUseCeval = function() {
    return this.vm.step.dests !== '' && (!this.vm.step.eval || !this.analyse.nextStepEvalBest(this.vm.path));
  }.bind(this);

  var startCeval = throttle(800, false, function() {
    if (this.ceval.enabled() && canUseCeval())
      this.ceval.start(this.vm.path, this.analyse.getSteps(this.vm.path));
  }.bind(this));

  this.toggleCeval = function() {
    this.ceval.toggle();
    this.setAutoShapes();
    startCeval();
  }.bind(this);

  this.showEvalGauge = function() {
    return this.hasAnyComputerAnalysis() && this.vm.showGauge() && this.vm.step.dests !== '';
  }.bind(this);

  this.hasAnyComputerAnalysis = function() {
    return this.data.analysis || this.ceval.enabled();
  }

  this.toggleAutoShapes = function(v) {
    if (this.vm.showAutoShapes(v)) this.setAutoShapes();
    else this.chessground.setAutoShapes([]);
  }.bind(this);

  this.toggleGauge = function(v) {
    this.vm.showGauge(!this.vm.showGauge());
  }.bind(this);

  this.setAutoShapes = function() {
    var s = this.vm.step,
      shapes = [],
      explorerUci = this.explorer.hoveringUci();
    if (explorerUci) shapes.push(makeAutoShapeFromUci(explorerUci, 'paleBlue'));
    if (this.vm.showAutoShapes()) {
      if (s.eval && s.eval.best) shapes.push(makeAutoShapeFromUci(s.eval.best, 'paleGreen'));
      if (!explorerUci) {
        var nextStepBest = this.analyse.nextStepEvalBest(this.vm.path);
        if (nextStepBest) shapes.push(makeAutoShapeFromUci(nextStepBest, 'paleBlue'));
        else if (this.ceval.enabled() && s.ceval && s.ceval.best) shapes.push(makeAutoShapeFromUci(s.ceval.best, 'paleBlue'));
      }
    }
    this.chessground.setAutoShapes(shapes);
  }.bind(this);

  var decomposeUci = function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4)];
  };

  var makeAutoShapeFromUci = function(uci, brush) {
    var move = decomposeUci(uci);
    return uci[1] === '@' ? {
      orig: move[1],
      brush: brush
    } : {
      orig: move[0],
      dest: move[1],
      brush: brush
    };
  }

  this.explorer = explorerCtrl(this, opts.explorer);

  this.explorerMove = function(uci) {
    var move = decomposeUci(uci);
    if (uci[1] === '@') this.chessground.apiNewPiece({
        color: this.chessground.data.movable.color,
        role: sanToRole[uci[0]]
      },
      move[1])
    else this.chessground.apiMove(move[0], move[1]);
    this.explorer.loading(true);
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);

  showGround();
  keyboard(this);
  startCeval();
  this.explorer.setStep();
};
