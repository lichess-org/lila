var m = require('mithril');
var socket = require('./socket');
var variant = require('./variant');
var lobby = require('./lobby');

module.exports = function(env) {

  this.data = env.data;
  lobby.sortHooks(this.data.hooks);

  this.socket = new socket(env.socketSend, this);

  var fixTab = function(tab) {
    if (['real_time', 'seeks', 'now_playing'].indexOf(tab) === -1) tab = 'real_time';
    if (tab === 'now_playing' && this.data.nowPlaying.length === 0) tab = 'real_time';
    return tab;
  }.bind(this);

  this.setTab = function(tab) {
    this.vm.tab = fixTab(tab);
    storage.set('lobbytab', this.vm.tab);
  }.bind(this);

  this.vm = {
    tab: fixTab(storage.get('lobbytab'))
  };

  this.clickHook = function(hook) {
    if (hook.action === 'cancel' || variant.confirm(data.variant)) socket.send(hook.action, hook.id);
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
