var m = require('mithril');

module.exports = function(env) {

  this.data = env.data;

  this.userId = env.userId;

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
