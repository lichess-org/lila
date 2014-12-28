var m = require('mithril');
var socket = require('./socket');
var variant = require('./variant');
var hookRepo = require('./hookRepo');
var seekRepo = require('./seekRepo');
var store = require('./store');
var xhr = require('./xhr');
var util = require('chessground').util;

module.exports = function(env) {

  this.data = env.data;
  this.data.hooks.forEach(hookRepo.init);
  hookRepo.sort(this);
  this.data.seeks.forEach(util.partial(seekRepo.init, this));
  seekRepo.sort(this);

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    tab: store.tab.get(),
    mode: store.mode.get(),
    filter: {
      open: false
    },
    stepHooks: this.data.hooks.slice(0),
    stepping: false
  };

  var flushHooksTimeout;

  this.flushHooks = function() {
    clearTimeout(flushHooksTimeout);
    this.vm.stepping = true;
    m.redraw();
    setTimeout(function() {
      this.vm.stepping = false;
      this.vm.stepHooks = this.data.hooks.slice(0);
      m.redraw();
    }.bind(this), 500);
    flushHooksSchedule();
  }.bind(this);

  var flushHooksSchedule = util.partial(setTimeout, this.flushHooks, 8000);
  flushHooksSchedule();

  this.setTab = function(tab) {
    if (tab === 'seeks' && tab !== this.vm.tab) xhr.seeks().then(this.setSeeks);
    this.vm.tab = store.tab.set(tab);
    this.vm.filter.open = false;
  }.bind(this);

  this.setMode = function(mode) {
    this.vm.mode = store.mode.set(mode);
    this.vm.filter.open = false;
  }.bind(this);

  this.toggleFilter = function() {
    this.vm.filter.open = !this.vm.filter.open;
  }.bind(this);

  this.setFilter = function(filter) {
    this.data.filter = filter;
  }.bind(this);

  this.clickHook = function(id) {
    if (this.vm.stepping) return;
    var hook = hookRepo.find(this, id);
    if (!hook || hook.disabled) return;
    if (hook.action === 'cancel' || variant.confirm(hook.variant)) this.socket.send(hook.action, hook.id);
  }.bind(this);

  this.clickSeek = function(id) {
    var seek = seekRepo.find(this, id);
    if (!seek) return;
    if (seek.action === 'cancelSeek' || variant.confirm(seek.variant)) this.socket.send(seek.action, seek.id);
  }.bind(this);

  this.setSeeks = function(seeks) {
    this.data.seeks = seeks;
    this.data.seeks.forEach(util.partial(seekRepo.init, this));
    seekRepo.sort(this);
  }.bind(this);

  this.router = env.routes;
  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
