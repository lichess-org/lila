var m = require('mithril');
var makeSocket = require('./socket');

module.exports = function(opts) {

  var lines = opts.lines;

  var vm = {
    isTroll: opts.kobold,
    isMod: opts.mod
  };

  var socket = makeSocket(opts.socketSend);

  return {
    lines: lines,
    vm: vm,
    trans: function(key) {
      return key;
    }
  };
};
