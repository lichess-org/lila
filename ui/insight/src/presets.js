var m = require('mithril');

module.exports = function (ctrl) {
  return m(
    'div.box.presets',
    ctrl.ui.presets.map(function (p) {
      var active = ctrl.makeUrl(p.dimension, p.metric, p.filters) === ctrl.makeCurrentUrl();
      return m(
        'a',
        {
          class: 'preset text' + (active ? ' active' : ''),
          'data-icon': '7',
          onclick: function () {
            ctrl.setQuestion(p);
          },
        },
        p.name
      );
    })
  );
};
