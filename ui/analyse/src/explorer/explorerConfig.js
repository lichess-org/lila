var m = require('mithril');
var partial = require('chessground').util.partial;
var storedProp = require('../util').storedProp;
var storedJsonProp = require('../util').storedJsonProp;

module.exports = {
  controller: function(variant, onClose) {
    var available = ['lichess'];
    if (variant.key === 'standard' || variant.key === 'fromPosition') {
      available.push('masters');
    }

    var data = {
      open: m.prop(false),
      db: {
        available: available,
        selected: available.length > 1 ? storedProp('explorer.db', available[0]) : function() {
          return available[0];
        }
      },
      rating: {
        available: [1600, 1800, 2000, 2200, 2500],
        selected: storedJsonProp('explorer.rating', [1600, 1800, 2000, 2200, 2500])
      },
      speed: {
        available: ['bullet', 'blitz', 'classical'],
        selected: storedJsonProp('explorer.speed', ['bullet', 'blitz', 'classical'])
      }
    };

    var toggleMany = function(c, value) {
      if (c().indexOf(value) === -1) c(c().concat([value]));
      else if (c().length > 1) c(c().filter(function(v) {
        return v !== value;
      }));
    };

    return {
      data: data,
      toggleOpen: function() {
        data.open(!data.open());
        if (!data.open()) onClose();
      },
      toggleDb: function(db) {
        data.db.selected(db);
      },
      toggleRating: partial(toggleMany, data.rating.selected),
      toggleSpeed: partial(toggleMany, data.speed.selected),
      fullHouse: function() {
        return data.db.selected() === 'masters' || (
          data.rating.selected().length === data.rating.available.length &&
          data.speed.selected().length === data.speed.available.length
        );
      }
    };
  },
  view: function(ctrl) {
    var d = ctrl.data;
    return [
      m('section.db', [
        m('label', 'Database'),
        m('div.choices', d.db.available.map(function(s) {
          return m('span', {
            class: d.db.selected() === s ? 'selected' : '',
            onclick: partial(ctrl.toggleDb, s)
          }, s);
        }))
      ]),
      d.db.selected() === 'masters' ? m('div.masters.message', [
        m('i[data-icon=C]'),
        m('p', "Two million OTB games"),
        m('p', "of 2200+ FIDE rated players"),
        m('p', "from 1952 to 2016"),
      ]) : m('div', [
        m('section.rating', [
          m('label', 'Players Average rating'),
          m('div.choices',
            d.rating.available.map(function(r) {
              return m('span', {
                class: d.rating.selected().indexOf(r) > -1 ? 'selected' : '',
                onclick: partial(ctrl.toggleRating, r)
              }, r);
            })
          )
        ]),
        m('section.speed', [
          m('label', 'Game speed'),
          m('div.choices',
            d.speed.available.map(function(s) {
              return m('span', {
                class: d.speed.selected().indexOf(s) > -1 ? 'selected' : '',
                onclick: partial(ctrl.toggleSpeed, s)
              }, s);
            })
          )
        ])
      ]),
      m('section.save',
        m('button.button.text[data-icon=E]', {
          onclick: ctrl.toggleOpen
        }, 'All set!')
      )
    ];
  }
};
