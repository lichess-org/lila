var k = require('mousetrap');

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
  init: function(ctrl) {
    k.bind(['left', 'h'], preventing(function() {
      ctrl.jump(ctrl.vm.ply - 1);
      m.redraw();
    }));
    k.bind(['right', 'l'], preventing(function() {
      ctrl.jump(ctrl.vm.ply + 1);
      m.redraw();
    }));
    k.bind(['up', 'j'], preventing(function() {
      ctrl.jump(1);
      m.redraw();
    }));
    k.bind(['down', 'k'], preventing(function() {
      ctrl.jump(ctrl.data.game.moves.length);
      m.redraw();
    }));
  }
};
