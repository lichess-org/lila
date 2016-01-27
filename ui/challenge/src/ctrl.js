var m = require('mithril');
var socket = require('./socket');
var xhr = require('./xhr');

module.exports = function(env) {

  this.data = env.data;

  this.userId = env.userId;

  this.socket = new socket(env.socketSend, this);

  this.vm = {
  };

  this.trans = lichess.trans(env.i18n);
};
