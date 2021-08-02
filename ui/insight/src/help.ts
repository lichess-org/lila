var m = require('mithril');

module.exports = function (ctrl) {
  return m('div.help.box', [
    m('div.top', 'Definitions'),
    m(
      'div.content',
      ['metric', 'dimension'].map(function (type) {
        var data = ctrl.vm[type];
        return m('section.' + type, [m('h3', data.name), m('p', m.trust(data.description))]);
      })
    ),
  ]);
};
