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

module.exports = function(ctrl) {
  k.bind('f', preventing(function() {
    ctrl.draughtsground.toggleOrientation();
    m.redraw();
  }));
};
