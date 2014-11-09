var control = require('./control');
var m = require('mithril');

module.exports = function(ctrl, elem) {
  $(elem).mousewheel(function(e) {
    if (e.deltaY == -1) control.next(ctrl);
    else if (e.deltaY == 1) control.prev(ctrl);
    m.redraw();
    e.stopPropagation();
    return false;
  });
};
