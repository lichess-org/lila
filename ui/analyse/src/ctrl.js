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
var router = require('game').router;
var game = require('game').game;
var m = require('mithril');

module.exports = function(opts) {

  this.data = data({}, opts.data);
  this.userId = opts.userId;
  this.ongoing = !util.synthetic(this.data) && game.playable(this.data);

  this.analyse = new analyse(this.data.steps);
  this.actionMenu = new actionMenu();
  this.autoplay = new autoplay(this);

  var initialPath = opts.path ? treePath.read(opts.path) : treePath.default(this.analyse.firstPly());
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
    showAutoShapes: util.storedProp('show-auto-shapes', true)
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
    var config = {
      fen: s.fen,
      turnColor: color,
      movable: {
        color: dests && Object.keys(dests).length > 0 ? color : null,
        dests: dests || {}
      },
      check: s.check,
      lastMove: s.uci ? [s.uci.substr(0, 2), s.uci.substr(2, 2)] : null,
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
      this.chessground = ground.make(this.data, config, userMove);
    this.chessground.set(config);
    if (opts.onChange) opts.onChange(config.fen, this.vm.path);
    if (!dests) getDests();
    setAutoShapesFromEval();
  }.bind(this);

  var getDests = throttle(200, false, function() {
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

  this.jump = function(path) {
    this.vm.path = path;
    this.vm.pathStr = treePath.write(path);
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#' + path[0].ply);
    showGround();
    if (!this.vm.step.uci) sound.move(); // initial position
    else if (this.vm.justPlayed !== this.vm.step.uci) {
      if (this.vm.step.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      this.vm.justPlayed = null;
    }
    if (/\+|\#/.test(this.vm.step.san)) sound.check();
    startCeval();
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
  }.bind(this);

  var forsyth = function(role) {
    return role === 'knight' ? 'n' : role[0];
  };

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
    // prepare premoving
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

  this.reset = function() {
    this.chessground.set(this.vm.situation);
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

  this.ceval = cevalCtrl(allowCeval,
    throttle(300, false, function(res) {
      this.analyse.updateAtPath(res.work.path, function(step) {
        if (step.ceval && step.ceval.depth >= res.eval.depth) return;
        step.ceval = res.eval;
        if (treePath.write(res.work.path) === this.vm.pathStr) {
          setAutoShapesFromEval();
          m.redraw();
        }
      }.bind(this));
    }.bind(this)));

  this.canUseCeval = function(step) {
    return step.dests !== '';
  }.bind(this);

  var startCeval = throttle(500, false, function() {
    if (this.ceval.enabled() && this.canUseCeval(this.vm.step))
      this.ceval.start(this.vm.path, this.analyse.getSteps(this.vm.path));
  }.bind(this));

  this.toggleCeval = function() {
    this.ceval.toggle();
    setAutoShapesFromEval();
    startCeval();
  }.bind(this);

  this.showEvalGauge = function() {
    return (this.data.analysis || this.ceval.enabled()) && this.vm.step.dests !== '';
  }.bind(this);

  this.toggleAutoShapes = function(v) {
    if (this.vm.showAutoShapes(v)) setAutoShapesFromEval();
    else this.chessground.setAutoShapes([]);
    console.log();
  }.bind(this);

  var setAutoShapesFromEval = function() {
    if (!this.vm.showAutoShapes()) return;
    var s = this.vm.step,
      shapes = [];
    if (s.eval && s.eval.best) shapes.push(makeAutoShapeFromUci(s.eval.best, 'paleGreen'));
    if (this.ceval.enabled() && s.ceval && s.ceval.best) shapes.push(makeAutoShapeFromUci(s.ceval.best, 'paleBlue'));
    this.chessground.setAutoShapes(shapes);
  }.bind(this);

  var makeAutoShapeFromUci = function(uci, brush) {
    return {
      orig: uci.slice(0, 2),
      dest: uci.slice(2, 4),
      brush: brush
    };
  };

  this.trans = lichess.trans(opts.i18n);

  showGround();
  keyboard(this);
  startCeval();
};
