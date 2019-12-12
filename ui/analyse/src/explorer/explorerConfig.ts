import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import { prop } from 'common';
import { storedProp, storedJsonProp } from 'common/storage';
import { bind, dataIcon } from '../util';
import { Game } from '../interfaces';
import { ExplorerDb, ExplorerSpeed, ExplorerConfigData, ExplorerConfigCtrl } from './interfaces';

const allSpeeds: ExplorerSpeed[] = ['bullet', 'blitz', 'rapid', 'classical'];
const allRatings = [1600, 1800, 2000, 2200, 2500];

export function controller(game: Game, onClose: () => void, trans: Trans, redraw: () => void): ExplorerConfigCtrl {

  const variant = (game.variant.key === 'fromPosition') ? 'standard' : game.variant.key;

  const available: ExplorerDb[] = ['lichess'];
  if (variant === 'standard') available.unshift('masters');

  const data: ExplorerConfigData = {
    open: prop(false),
    db: {
      available,
      selected: available.length > 1 ? storedProp('explorer.db.' + variant, available[0]) : function() {
        return available[0];
      }
    },
    rating: {
      available: allRatings,
      selected: storedJsonProp('explorer.rating', allRatings)
    },
    speed: {
      available: allSpeeds,
      selected: storedJsonProp<ExplorerSpeed[]>('explorer.speed', allSpeeds)
    }
  };

  const toggleMany = function(c, value) {
    if (!c().includes(value)) c(c().concat([value]));
    else if (c().length > 1) c(c().filter(v => v !== value));
  };

  return {
    trans,
    redraw,
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

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  const d = ctrl.data;
  return [
    h('section.db', [
      h('label', ctrl.trans.noarg('database')),
      h('div.choices', d.db.available.map(function(s) {
        return h('span', {
          class: { selected: d.db.selected() === s },
          hook: bind('click', _ => ctrl.toggleDb(s), ctrl.redraw)
        }, s);
      }))
    ]),
    d.db.selected() === 'masters' ? h('div.masters.message', [
      h('i', { attrs: dataIcon('C') }),
      h('p', ctrl.trans('masterDbExplanation', 2200, '1952', '2019'))
    ]) : h('div', [
      h('section.rating', [
        h('label', ctrl.trans.noarg('averageElo')),
        h('div.choices',
          d.rating.available.map(function(r) {
            return h('span', {
              class: { selected: d.rating.selected().includes(r) },
              hook: bind('click', _ => ctrl.toggleRating(r), ctrl.redraw)
            }, r.toString());
          })
        )
      ]),
      h('section.speed', [
        h('label', ctrl.trans.noarg('timeControl')),
        h('div.choices',
          d.speed.available.map(function(s) {
            return h('span', {
              class: { selected: d.speed.selected().includes(s) },
              hook: bind('click', _ => ctrl.toggleSpeed(s), ctrl.redraw)
            }, s);
          })
        )
      ])
    ]),
    h('section.save',
      h('button.button.button-green.text', {
        attrs: dataIcon('E'),
        hook: bind('click', ctrl.toggleOpen)
      }, ctrl.trans.noarg('allSet'))
    )
  ];
}
