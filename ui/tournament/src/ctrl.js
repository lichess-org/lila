var m = require('mithril');
var socket = require('./socket');
var xhr = require('./xhr');
var pagination = require('./pagination');
var util = require('chessground').util;
var sound = require('./sound');
var myCurrentGameId = require('./tournament').myCurrentGameId;

module.exports = function(env) {

  this.data = env.data;

  this.userId = env.userId;

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    page: this.data.standing.page,
    pages: {},
    focusOnMe: !!this.data.me,
    joinLoader: false
  };

  this.reload = function(data) {
    if (this.data.isStarted !== data.isStarted) m.redraw.strategy('all');
    this.data = data;
    this.loadPage(data.standing);
    if (this.vm.focusOnMe) this.scrollToMe();
    startWatching();
    sound.end(this.data);
    sound.countDown(this.data);
    this.vm.joinLoader = false;
    redirectToMyGame();
  }.bind(this);

  var redirectToMyGame = function() {
    var gameId = myCurrentGameId(this);
    if (gameId && lichess.storage.get('last-game') !== gameId)
      location.href = '/' + gameId;
  }.bind(this);

  this.loadPage = function(data) {
    this.vm.pages[data.page] = data.players;
  }.bind(this);
  this.loadPage(this.data.standing);

  var setPage = function(page) {
    m.redraw.strategy('all');
    this.vm.page = page;
    xhr.loadPage(this, page)
  }.bind(this);

  this.userSetPage = function(page) {
    this.vm.focusOnMe = false;
    setPage(page);
  }.bind(this);

  this.withdraw = function() {
    xhr.withdraw(this);
    this.vm.joinLoader = true;
    this.vm.focusOnMe = false;
  }.bind(this);

  this.join = function() {
    xhr.join(this);
    this.vm.joinLoader = true;
    this.vm.focusOnMe = true;
  }.bind(this);

  var alreadyWatching = [];
  var startWatching = function() {
    var newIds = this.data.lastGames.map(function(p) {
      return p.id;
    }).filter(function(id) {
      return alreadyWatching.indexOf(id) === -1;
    });
    if (newIds.length) {
      setTimeout(function() {
        this.socket.send("startWatching", newIds.join(' '));
      }.bind(this), 1000);
      newIds.forEach(alreadyWatching.push.bind(alreadyWatching));
    }
  }.bind(this);
  startWatching();

  this.scrollToMe = function() {
    if (!this.data.me) return;
    var page = pagination.myPage(this);
    if (page !== this.vm.page) setPage(page);
  }.bind(this);
  this.scrollToMe();

  this.toggleFocusOnMe = function() {
    if (!this.data.me) return;
    this.vm.focusOnMe = !this.vm.focusOnMe;
    if (this.vm.focusOnMe) this.scrollToMe();
  }.bind(this);

  sound.end(this.data);
  sound.countDown(this.data);
  redirectToMyGame();

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
