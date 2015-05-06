var chessground = require('chessground');
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
var socket = require('./socket');
var m = require('mithril');

module.exports = function(opts) {

  this.data = data({}, opts.data);
  this.analyse = new analyse(this.data.steps);
  this.actionMenu = new actionMenu();
  this.autoplay = new autoplay(this);

  this.userId = opts.userId;

  var initialPath = opts.path ? treePath.read(opts.path) : treePath.default();

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
      this.vm.path = treePath.default();
      s = this.analyse.getStep(this.vm.path);
    }
    var color = s.ply % 2 === 0 ? 'white' : 'black';
    var dests = readDests(s.dests);
    var config = {
      fen: s.fen,
      turnColor: color,
      movable: {
        color: Object.keys(dests).length === 0 ? null : color,
        dests: dests
      },
      check: s.check,
      lastMove: s.uci ? [s.uci.substr(0, 2), s.uci.substr(2, 2)] : null,
    };
    this.vm.step = s;
    this.vm.cgConfig = config;
    if (!this.chessground)
      this.chessground = ground.make(this.data, config, userMove);
    this.chessground.stop();
    this.chessground.set(config);
    if (opts.onChange) opts.onChange(config.fen, this.vm.path);
  }.bind(this);

  this.jump = function(path) {
    this.vm.path = path;
    this.vm.pathStr = treePath.write(path);
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#' + path[0].ply);
    showGround();
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

  var forsyth = function(role) {
    return role === 'knight' ? 'n' : role[0];
  };

  var userMove = function(orig, dest) {
    $.sound.move();
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
  }.bind(this);

  this.addStep = function(step, path) {
    this.userJump(this.analyse.addStep(step, treePath.read(path)));
    m.redraw();
  }.bind(this);

  this.reset = function() {
    this.chessground.set(this.vm.situation);
    m.redraw();
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
