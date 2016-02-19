var k = Mousetrap;
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

function prev(ctrl) {
  ctrl.userJump(ctrl.vm.ply - 1);
}

function next(ctrl) {
  ctrl.userJump(ctrl.vm.ply + 1);
}

module.exports = {
  prev: prev,
  next: next,
  init: function(ctrl) {
    k.bind(['left', 'h'], preventing(function() {
      prev(ctrl);
      m.redraw();
    }));
    k.bind(['right', 'l'], preventing(function() {
      next(ctrl);
      m.redraw();
    }));
    k.bind(['up', 'k'], preventing(function() {
      ctrl.userJump(1);
      m.redraw();
    }));
    k.bind(['down', 'j'], preventing(function() {
      ctrl.userJump(ctrl.data.steps.length - 1);
      m.redraw();
    }));
    k.bind('f', preventing(ctrl.flip));
  }
};
