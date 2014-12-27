var m = require('mithril');
var partial = chessground.util.partial;

module.exports = function(opts) {

  this.data = data({}, opts.data);

  this.vm = {
  };

  this.socket = new socket(opts.socketSend, this);

  this.router = opts.routes;

  this.trans = function() {
    var str = opts.i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  init(this);
};
