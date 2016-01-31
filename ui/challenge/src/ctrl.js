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

  this.decline = function(id) {
    this.data.in.forEach(function(c) {
      if (c.id === id) {
        xhr.decline(id).then(this.update);
        c.declined = true;
      }
    });
  }.bind(this);

  xhr.load().then(this.update);

  this.trans = lichess.trans(env.i18n);
};
