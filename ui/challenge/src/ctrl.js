var m = require('mithril');
var socket = require('./socket');
var xhr = require('./xhr');

module.exports = function(env) {

  this.data = env.data;

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    initiating: true,
    reloading: false
  };

  this.update = function(data) {
    this.data = data;
    this.vm.initiating = false;
    this.vm.reloading = false;
  }.bind(this);

  xhr.load().then(this.update);

  this.trans = lichess.trans(env.i18n);
};
