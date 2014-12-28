var m = require('mithril');
var socket = require('./socket');
var variant = require('./variant');
var hookRepo = require('./hookRepo');
var store = require('./store');

module.exports = function(env) {

  this.data = env.data;
  hookRepo.sort(this);

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    tab: store.tab.get(),
    stepHooks: hookRepo.stepSlice(this),
    stepping: false
  };

  var flushHooksTimeout;

  this.flushHooks = function() {
    clearTimeout(flushHooksTimeout);
    this.vm.stepping = true;
    m.redraw();
    setTimeout(function() {
      this.vm.stepping = false;
      this.vm.stepHooks = hookRepo.stepSlice(this);
      m.redraw();
    }.bind(this), 500);
    flushHooksSchedule();
  }.bind(this);

  var flushHooksSchedule = function() {
    flushHooksTimeout = setTimeout(this.flushHooks, 8000);
  }.bind(this);
  flushHooksSchedule();

  this.setTab = function(tab) {
    this.vm.tab = store.tab.set(tab);
  }.bind(this);

  this.clickHook = function(hook) {
    if (this.vm.stepping) return;
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
