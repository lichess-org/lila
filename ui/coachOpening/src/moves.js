var m = require('mithril');

module.exports = function(ctrl, family) {
  var d = ctrl.data;
  return m('div', d.openings.map[family].moves[ctrl.data.color].map(function(m) {
    return m.nb;
  }));
};
