var control = require('./control');
var partial = require('chessground').util.partial;
var m = require('mithril');

module.exports = function(ctrl) {

  var timeout;

  this.delay = null;

  var move = function() {
    if (control.canGoForward(ctrl)) {
      control.next(ctrl);
      m.redraw();
      return true;
    }
    this.stop();
    m.redraw();
    return false;
  }.bind(this);

  var nextDelay = function() {
    if (typeof(this.delay) === 'string') {
      // in a variation
      if (!ctrl.tree.pathIsMainline(ctrl.vm.path)) return 2000;
      if (this.delay === 'realtime') {
        return (ctrl.data.game.moveTimes[ctrl.vm.node.ply] * 100) || 2000;
      } else {
        var slowDown = this.delay === 'cpl_fast' ? 10 : 50;
        if (ctrl.vm.node.ply >= ctrl.vm.mainline.length - 1) return 0;
        var currEval = ctrl.vm.node.eval;
        var currPlyCp = currEval.mate ? 990 : currEval.cp;
        var nextEval = ctrl.vm.node.children[0] ? ctrl.vm.node.children[0].eval : null;
        // if nextEval is undefined, the next move is a checkmate
        var nextPlyCp = nextEval ? (nextEval.mate ? 990 : nextEval.cp) : 990;
        return Math.abs(currPlyCp - nextPlyCp) * slowDown;
      }
    }
    return this.delay;
  }.bind(this);

  var schedule = function() {
    timeout = setTimeout(function() {
      if (move()) schedule();
    }, nextDelay());
  }.bind(this);

  var start = function(delay) {
    this.delay = delay;
    this.stop();
    schedule();
  }.bind(this);

  this.stop = function() {
    if (timeout) {
      clearTimeout(timeout);
      timeout = null;
    }
  }.bind(this);

  this.toggle = function(delay) {
    if (this.active(delay)) this.stop();
    else {
      if (!this.active())
        if (!move()) ctrl.jump('');
      start(delay);
    }
  }.bind(this);

  this.active = function(delay) {
    return (!delay || delay === this.delay) && !!timeout;
  }.bind(this);
};
