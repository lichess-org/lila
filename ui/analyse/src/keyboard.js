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
  k.bind(['left', 'k'], preventing(function() {
    control.prev(ctrl);
    m.redraw();
  }));
  k.bind(['shift+left', 'shift+k'], preventing(function() {
    control.exitVariation(ctrl);
    m.redraw();
  }));
  k.bind(['right', 'j'], preventing(function() {
    if (!ctrl.fork.proceed()) control.next(ctrl);
    m.redraw();
  }));
  k.bind(['shift+right', 'shift+j'], preventing(function() {
    control.enterVariation(ctrl);
    m.redraw();
  }));
  k.bind(['up', 'h', '0'], preventing(function() {
    if (!ctrl.fork.prev()) control.first(ctrl);
    m.redraw();
  }));
  k.bind(['down', 'l', '$'], preventing(function() {
    if (!ctrl.fork.next()) control.last(ctrl);
    m.redraw();
  }));
  k.bind('c', preventing(function() {
    ctrl.vm.comments = !ctrl.vm.comments;
    m.redraw();
    ctrl.autoScroll();
  }));
  k.bind(['esc'], ctrl.chessground.cancelMove);
  k.bind('f', preventing(ctrl.flip));
};
