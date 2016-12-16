var m = require('mithril');

module.exports = function(root) {
  var ctrl = root.retro;
  if (!ctrl) return;
  return m('div.retro_box', [
    "retrospect"
  ]);
};
