var k = require('mousetrap');
var control = require('./control');

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
  k.bind(['up', 'j'], preventing(function() {
    control.first(ctrl);
    m.redraw();
  }));
  k.bind(['down', 'k'], preventing(function() {
    control.last(ctrl);
    m.redraw();
  }));
};
