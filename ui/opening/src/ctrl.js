var m = require('mithril');
var chessground = require('chessground');

module.exports = function(cfg, router, i18n) {

  this.data = cfg;

  this.chessground = new chessground.controller({
    fen: this.data.opening.fen,
    orientation: this.data.opening.color,
    viewOnly: true,
  });

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
