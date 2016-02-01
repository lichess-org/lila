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

  this.countActiveIn = function() {
    return this.data.in.filter(function(c) {
      return !c.declined;
    }).length;
  }

  this.update = function(data) {
    console.log(data, 'update');
    this.data = data;
    this.vm.initiating = false;
    this.vm.reloading = false;
    env.setCount(this.countActiveIn());
    m.redraw();
  }.bind(this);

  this.decline = function(id) {
    this.data.in.forEach(function(c) {
      if (c.id === id) {
        c.declined = true;
        xhr.decline(id).then(this.update);
      }
    }.bind(this));
  }.bind(this);

  xhr.load().then(this.update);

  this.trans = lichess.trans(env.i18n);
};
