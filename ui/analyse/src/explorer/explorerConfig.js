var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {
  var c = ctrl.explorer.config;
  return m('div.config', [
    m('div.title', 'Opening explorer configuration'),
    m('section.db', [
      m('label', 'Database'),
      m('div.choices',
        c.db.available.map(function(s) {
          return m('span', {
            class: c.db.selected() === s ? 'selected' : '',
            onclick: partial(ctrl.explorer.toggleDb, s)
          }, s);
        })
      )
    ]),
    m('section.rating', [
      m('label', 'Players Average rating'),
      m('div.choices',
        c.rating.available.map(function(r) {
          return m('span', {
            class: c.rating.selected().indexOf(r) > -1 ? 'selected' : '',
            onclick: partial(ctrl.explorer.toggleRating, r)
          }, r);
        })
      )
    ]),
    m('section.speed', [
      m('label', 'Game speed'),
      m('div.choices',
        c.speed.available.map(function(s) {
          return m('span', {
            class: c.speed.selected().indexOf(s) > -1 ? 'selected' : '',
            onclick: partial(ctrl.explorer.toggleSpeed, s)
          }, s);
        })
      )
    ])
  ]);
};
