var k = Mousetrap;
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

module.exports = function(ctrl) {
  k.bind(['left', 'h'], preventing(function() {
    control.prev(ctrl);
    m.redraw();
  }));
  k.bind(['shift+left', 'shift+h'], preventing(function() {
    control.exitVariation(ctrl);
    m.redraw();
  }));
  k.bind(['right', 'l'], preventing(function() {
    control.next(ctrl);
    m.redraw();
  }));
  k.bind(['shift+right', 'shift+l'], preventing(function() {
    control.enterVariation(ctrl);
    m.redraw();
  }));
  k.bind(['up', 'k'], preventing(function() {
    control.first(ctrl);
    m.redraw();
  }));
  k.bind(['down', 'j'], preventing(function() {
    control.last(ctrl);
    m.redraw();
  }));
  k.bind('c', preventing(function() {
    ctrl.vm.comments = !ctrl.vm.comments;
    m.redraw();
  }));
  k.bind(['esc'], ctrl.chessground.cancelMove);
  k.bind('f', preventing(ctrl.flip));
};
