var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {
  var c = ctrl.explorer.config;
  return m('div.config', [
    m('section.rating', [
      m('label', 'Average ratings'),
      m('div.choices',
        c.rating.available.map(function(r) {
          return m('span', {
            class: c.rating.selected().indexOf(r) > -1 ? 'selected' : '',
            onclick: partial(ctrl.explorer.toggleRating,r)
          }, r);
        })
      )
    ])
  ]);
};
