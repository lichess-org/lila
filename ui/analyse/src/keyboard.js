var k = require('mousetrap');
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
  k.bind(['right', 'l'], preventing(function() {
    control.next(ctrl);
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
    if (!ctrl.vm.comments && ctrl.vm.path.length > 1) {
      path = [ctrl.vm.path[0]];
      path[0].variation = null;
      ctrl.jump(path);
    }
    m.redraw();
  }));
  k.bind(['esc'], ctrl.chessground.cancelMove);
};
