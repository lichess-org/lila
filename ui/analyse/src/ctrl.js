var m = require('mithril');
var chessground = require('chessground');
var data = require('./data');
var ground = require('./ground');

module.exports = function(cfg, router, i18n, socketSend) {

  this.data = data({}, cfg);

  this.vm = {
    flip: false,
    ply: 1
  };

  this.flip = function() {
    this.vm.flip = !this.vm.flip;
    this.chessground.set({
      orientation: this.vm.flip ? this.data.opponent.color : this.data.player.color
    });
  }.bind(this);

  this.chessground = ground.make(this.data, cfg.game.fen);

  this.router = router;

  this.trans = function() {
    var str = i18n[arguments[0]]
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
