var m = require('mithril');

module.exports = function(opts) {

  this.data = opts.data;
  window.openingData = opts.data;

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
