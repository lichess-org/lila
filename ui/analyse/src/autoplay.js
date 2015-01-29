var control = require('./control');
var partial = require('chessground').util.partial;
var m = require('mithril');

module.exports = function(ctrl) {

  var interval;

  this.delay = null;

  var next = function() {
    if (control.canGoForward(ctrl)) {
      var p = ctrl.vm.path;
      p[p.length - 1].ply++;
      ctrl.jump(p);
      m.redraw();
    } else this.stop();
  }.bind(this);

  var start = function(delay) {
    this.delay = delay;
    this.stop();
    interval = setInterval(next, this.delay);
  }.bind(this);

  this.stop = function() {
    if (interval) {
      clearInterval(interval);
      interval = null;
    }
  }.bind(this);

  this.toggle = function(delay) {
    if (this.active(delay)) this.stop();
    else {
      if (!this.active()) next();
      start(delay);
    }
  }.bind(this);

  this.active = function(delay) {
    return (!delay || delay === this.delay) && !!interval;
  }.bind(this);
};
