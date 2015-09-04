var chessground = require('chessground');
var opposite = chessground.util.opposite;
var data = require('./data');
var analyse = require('./analyse');
var ground = require('./ground');
var keyboard = require('./keyboard');
var treePath = require('./path');
var actionMenu = require('./actionMenu').controller;
var autoplay = require('./autoplay');
var control = require('./control');
var promotion = require('./promotion');
var readDests = require('./util').readDests;
var throttle = require('./util').throttle;
var socket = require('./socket');
var m = require('mithril');

module.exports = function(opts) {

  this.data = data({}, opts.data);
  this.analyse = new analyse(this.data.steps);
  this.actionMenu = new actionMenu();
  this.autoplay = new autoplay(this);

  this.userId = opts.userId;

  var initialPath = opts.path ? treePath.read(opts.path) : treePath.default(this.analyse.firstPly());

  this.vm = {
    path: initialPath,
    pathStr: treePath.write(initialPath),
    step: null,
    cgConfig: null,
    comments: true,
    flip: false
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
    var dests = readDests(s.dests);
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
    if (path === this.vm.pathStr) showGround();
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

  this.router = opts.routes;

  this.trans = function(key) {
    var str = opts.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  showGround();
  keyboard(this);
};
