var control = require('./control');
var partial = require('chessground').util.partial;
var m = require('mithril');

module.exports = function(ctrl) {

  var interval;
  var delay = 1000;

  var next = function() {
    if (control.canGoForward(ctrl)) {
      var p = ctrl.vm.path;
      p[p.length - 1].ply++;
      ctrl.jump(p);
      m.redraw();
    } else this.stop();
  }.bind(this);

  this.start = function() {
    this.stop();
    next();
    interval = setInterval(next, delay);
  }.bind(this);

  this.stop = function() {
    if (interval) {
      clearInterval(interval);
      interval = null;
    }
  }.bind(this);

  this.toggle = function() {
    if (this.active()) this.stop();
    else this.start();
  }.bind(this);

  this.active = function() {
    return !!interval;
  }.bind(this);
};
