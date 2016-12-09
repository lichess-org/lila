var control = require('./control');
var m = require('mithril');

function preventing(f) {
  return function(e) {
    if (e.preventDefault) {
      e.preventDefault();
    } else {
      // internet explorer
      e.returnValue = false;
    }
    f();
  };
}

module.exports = {
  bind: function(ctrl) {
    if (!window.Mousetrap) return;
    var kbd = window.Mousetrap;
    kbd.bind(['left', 'k'], preventing(function() {
      control.prev(ctrl);
      m.redraw();
    }));
    kbd.bind(['right', 'j'], preventing(function() {
      control.next(ctrl);
      m.redraw();
    }));
    kbd.bind(['up', '0'], preventing(function() {
      control.first(ctrl);
      m.redraw();
    }));
    kbd.bind(['down', '$'], preventing(function() {
      control.last(ctrl);
      m.redraw();
    }));
    kbd.bind('l', preventing(ctrl.toggleCeval));
    kbd.bind('space', preventing(function() {
      if (ctrl.vm.mode !== 'view') return;
      if (ctrl.getCeval().enabled()) ctrl.playBestMove();
      else ctrl.toggleCeval();
    }));
  }
};
