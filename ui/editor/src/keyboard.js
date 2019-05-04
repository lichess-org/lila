var k = Mousetrap;
var m = require('mithril');

module.exports = function(ctrl) {
  k.bind('f', function(e) {
    e.preventDefault();
    ctrl.chessground.toggleOrientation();
    m.redraw();
  });
};
