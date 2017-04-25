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
    var k = Mousetrap;
    k.bind(['left', 'h'], preventing(function() {
      prev(ctrl);
      ctrl.redraw();
    }));
    k.bind(['right', 'l'], preventing(function() {
      next(ctrl);
      ctrl.redraw();
    }));
    k.bind(['up', 'k'], preventing(function() {
      ctrl.userJump(1);
      ctrl.redraw();
    }));
    k.bind(['down', 'j'], preventing(function() {
      ctrl.userJump(ctrl.data.steps.length - 1);
      ctrl.redraw();
    }));
    k.bind('f', preventing(ctrl.flip));
  }
};
