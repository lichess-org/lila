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
  this.playban = env.playban;
  this.currentGame = env.currentGame;
  this.perfIcons = env.perfIcons;

  hookRepo.initAll(this);
  seekRepo.initAll(this);

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    tab: store.tab.get(),
    mode: store.mode.get(),
    sort: store.sort.get(),
    filterOpen: false,
    stepHooks: this.data.hooks.slice(0),
    stepping: false,
    redirecting: false
  };

  var flushHooksTimeout;

  var doFlushHooks = function() {
    this.vm.stepHooks = this.data.hooks.slice(0);
    if (this.vm.tab === 'real_time') m.redraw();
  }.bind(this);

  this.flushHooks = function(now) {
    clearTimeout(flushHooksTimeout);
    if (now) doFlushHooks();
    else {
      this.vm.stepping = true;
      if (this.vm.tab === 'real_time') m.redraw();
      setTimeout(function() {
        this.vm.stepping = false;
        doFlushHooks();
      }.bind(this), 500);
    }
    flushHooksTimeout = flushHooksSchedule();
  }.bind(this);

  var flushHooksSchedule = util.partial(setTimeout, this.flushHooks, 8000);
  flushHooksSchedule();

  this.setTab = function(tab) {
    if (tab === 'seeks' && tab !== this.vm.tab) xhr.seeks().then(this.setSeeks);
    this.vm.tab = store.tab.set(tab);
    this.vm.filterOpen = false;
  }.bind(this);

  this.setMode = function(mode) {
    this.vm.mode = store.mode.set(mode);
    this.vm.filterOpen = false;
  }.bind(this);

  this.setSort = function(sort) {
    this.vm.sort = store.sort.set(sort);
  }.bind(this);

  this.toggleFilter = function() {
    this.vm.filterOpen = !this.vm.filterOpen;
  }.bind(this);

  this.setFilter = function(filter) {
    this.data.filter = filter;
    this.flushHooks(true);
    if (this.vm.tab !== 'real_time') m.redraw();
  }.bind(this);

  this.clickHook = function(id) {
    var hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.vm.stepping || this.vm.redirecting) return;
    if (hook.action === 'cancel' || variant.confirm(hook.variant)) this.socket.send(hook.action, hook.id);
  }.bind(this);

  this.clickSeek = function(id) {
    var seek = seekRepo.find(this, id);
    if (!seek || this.vm.redirecting) return;
    if (seek.action === 'cancelSeek' || variant.confirm(seek.variant)) this.socket.send(seek.action, seek.id);
  }.bind(this);

  this.setSeeks = function(seeks) {
    this.data.seeks = seeks;
    seekRepo.initAll(this);
  }.bind(this);

  this.gameActivity = function(gameId) {
    if (this.data.nowPlaying.filter(function(p) {
      return p.gameId === gameId;
    }).length) xhr.nowPlaying().then(function(povs) {
      this.data.nowPlaying = povs;
      this.startWatching();
    }.bind(this));
  }.bind(this);

  var alreadyWatching = [];
  this.startWatching = function() {
    var newIds = this.data.nowPlaying.map(function(p) {
      return p.gameId;
    }).filter(function(id) {
      return alreadyWatching.indexOf(id) === -1;
    });
    if (newIds.length) {
      setTimeout(function() {
        this.socket.send("startWatching", newIds.join(' '));
      }.bind(this), 2000);
      newIds.forEach(alreadyWatching.push.bind(alreadyWatching));
    }
  }.bind(this);

  this.startWatching();

  this.setRedirecting = function() {
    this.vm.redirecting = true;
    setTimeout(function() {
      this.vm.redirecting = false;
    }.bind(this), 2000);
  }.bind(this);

  this.trans = lichess.trans(env.i18n);

  if (this.playban) setTimeout(location.reload, this.playban.remainingSeconds * 1000);
};
