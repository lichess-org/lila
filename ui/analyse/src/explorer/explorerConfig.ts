import { prop, storedProp, storedJsonProp } from 'common';
import { bind, dataIcon } from '../util';
import { h } from 'snabbdom'

export function controller(game, withGames, onClose) {

  const variant = (game.variant.key === 'fromPosition') ? 'standard' : game.variant.key;

  const available = ['lichess'];
  if (variant === 'standard') available.push('masters');
  else if (variant === 'antichess' && withGames && game.initialFen.indexOf('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w ') === 0) {
    available.push('watkins');
  }

  const data = {
    open: prop(false),
    db: {
      available,
      selected: available.length > 1 ? storedProp('explorer.db.' + variant, available[0]) : function() {
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

  const toggleMany = function(c, value) {
    if (c().indexOf(value) === -1) c(c().concat([value]));
    else if (c().length > 1) c(c().filter(v => v !== value));
  };

  return {
    data,
    toggleOpen() {
      data.open(!data.open());
      if (!data.open()) onClose();
    },
    toggleDb(db) {
      data.db.selected(db);
    },
    toggleRating(v) { toggleMany(data.rating.selected, v) },
    toggleSpeed(v) { toggleMany(data.speed.selected, v) },
    fullHouse() {
      return data.db.selected() === 'masters' || (
        data.rating.selected().length === data.rating.available.length &&
          data.speed.selected().length === data.speed.available.length
      );
    }
  };
}

export function view(ctrl) {
  const d = ctrl.data;
  return [
    h('section.db', [
      h('label', 'Database'),
      h('div.choices', d.db.available.map(function(s) {
        return h('span', {
          class: { selected: d.db.selected() === s },
          hook: bind('click', _ => ctrl.toggleDb(s))
        }, s);
      }))
    ]),
    d.db.selected() === 'masters' ? h('div.masters.message', [
      h('i', { attrs: dataIcon('C') }),
      h('p', "Two million OTB games of 2200+ FIDE rated players from 1952 to 2016"),
    ]) : (d.db.selected() === 'watkins' ? h('div.masters.message', [
      h('i', { attrs: dataIcon('@') }),
      h('p', "Watkins antichess solution: 1. e3 is a win for white")
    ]) : h('div', [
      h('section.rating', [
        h('label', "Players' average rating"),
        h('div.choices',
          d.rating.available.map(function(r) {
            return h('span', {
              class: { selected: d.rating.selected().indexOf(r) > -1 },
              hook: bind('click', _ => ctrl.toggleRating(r))
            }, r);
          })
        )
      ]),
      h('section.speed', [
        h('label', 'Game speed'),
        h('div.choices',
          d.speed.available.map(function(s) {
            return h('span', {
              class: { selected: d.speed.selected().indexOf(s) > -1 },
              hook: bind('click', _ => ctrl.toggleSpeed(s))
            }, s);
          })
        )
      ])
    ])),
    h('section.save',
      h('button.button.text', {
        attrs: dataIcon('E'),
        hook: bind('click', ctrl.toggleOpen)
      }, 'All set!')
    )
  ];
}
