var m = require('mithril');
var socket = require('./socket');
var xhr = require('./xhr');
var util = require('chessground').util;

module.exports = function(env) {

  this.data = env.data;

  this.userId = env.userId;

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    loading: false
  };

  this.reload = function(data) {
    this.data = data;
    this.vm.loading = false;
    startWatching();
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

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
