var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.box.presets',
    ctrl.ui.presets.map(function(p) {
      return m('a.preset.text', {
        'data-icon': '7',
        onclick: function() {
          ctrl.setQuestion(p);
        }
      }, p.name);
    })
  );
};
