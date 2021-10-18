import { h, VNode } from 'snabbdom';
import { prop } from 'common';
import { bind, dataIcon, onInsert } from 'common/snabbdom';
import { storedProp, storedJsonProp, StoredJsonProp } from 'common/storage';
import { Game } from '../interfaces';
import { ExplorerDb, ExplorerSpeed, ExplorerConfigData, ExplorerConfigCtrl } from './interfaces';
import { snabModal } from 'common/modal';

const allSpeeds: ExplorerSpeed[] = ['bullet', 'blitz', 'rapid', 'classical'];
const allRatings = [1600, 1800, 2000, 2200, 2500];

export function controller(game: Game, onClose: () => void, trans: Trans, redraw: () => void): ExplorerConfigCtrl {
  const variant = game.variant.key === 'fromPosition' ? 'standard' : game.variant.key;

  const available: ExplorerDb[] = ['lichess', 'player'];
  if (variant === 'standard') available.unshift('masters');

  const data: ExplorerConfigData = {
    open: prop(true),
    db: {
      available,
      selected:
        available.length > 1
          ? storedProp('explorer.db.' + variant, available[0])
          : function () {
              return available[0];
            },
    },
    rating: {
      available: allRatings,
      selected: storedJsonProp('explorer.rating', () => allRatings),
    },
    speed: {
      available: allSpeeds,
      selected: storedJsonProp<ExplorerSpeed[]>('explorer.speed', () => allSpeeds),
    },
    playerName: {
      open: prop(false),
      value: storedProp<string | undefined>('explorer.player.name', document.body.dataset['user'] || ''),
    },
  };

  const toggleMany = function <T>(c: StoredJsonProp<T[]>, value: T) {
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
    toggleRating(v) {
      toggleMany(data.rating.selected, v);
    },
    toggleSpeed(v) {
      toggleMany(data.speed.selected, v);
    },
    fullHouse() {
      return (
        data.db.selected() === 'masters' ||
        (data.rating.selected().length === data.rating.available.length &&
          data.speed.selected().length === data.speed.available.length)
      );
    },
  };
}

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  return [
    h('section.db', [
      h('label', ctrl.trans.noarg('database')),
      h(
        'div.choices',
        ctrl.data.db.available.map(s =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.data.db.selected() === s}`,
              },
              hook: bind('click', _ => ctrl.toggleDb(s), ctrl.redraw),
            },
            s
          )
        )
      ),
    ]),
    ctrl.data.db.selected() === 'masters'
      ? masterDb(ctrl)
      : ctrl.data.db.selected() === 'lichess'
      ? lichessDb(ctrl)
      : playerDb(ctrl),
    h(
      'section.save',
      h(
        'button.button.button-green.text',
        {
          attrs: dataIcon(''),
          hook: bind('click', ctrl.toggleOpen),
        },
        ctrl.trans.noarg('allSet')
      )
    ),
  ];
}

const playerDb = (ctrl: ExplorerConfigCtrl) => {
  const name = ctrl.data.playerName.value();
  return h('div.player-db', [
    ctrl.data.playerName.open() ? playerModal(ctrl) : undefined,
    h('section.name', [
      name
        ? h(
            'span.user-link.ulpt',
            {
              hook: onInsert(lichess.powertip.manualUser),
              attrs: { 'data-href': `/@/${name}` },
            },
            name
          )
        : undefined,
      h(
        `button.${name ? 'button-link' : 'button'}`,
        {
          hook: bind('click', () => ctrl.data.playerName.open(true), ctrl.redraw),
        },
        name ? 'Change' : 'Select a Lichess player'
      ),
    ]),
  ]);
};

const playerModal = (ctrl: ExplorerConfigCtrl) => {
  return snabModal({
    class: 'explorer__config__player__choice',
    onClose() {
      ctrl.data.playerName.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Personal opening explorer'),
      h('div.input-wrapper', [
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(input =>
            lichess.userComplete().then(uac => {
              uac({
                input,
                tag: 'span',
                onSelect(v) {
                  // input.value = v.name;
                  ctrl.data.playerName.value(v.name);
                  ctrl.data.playerName.open(false);
                  ctrl.redraw();
                },
              });
              input.focus();
            })
          ),
        }),
      ]),
    ],
  });
};

const masterDb = (ctrl: ExplorerConfigCtrl) =>
  h('div.masters.message', [
    h('i', { attrs: dataIcon('') }),
    h('p', ctrl.trans('masterDbExplanation', 2200, '1952', '2019')),
  ]);

const lichessDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    h('section.rating', [
      h('label', ctrl.trans.noarg('averageElo')),
      h(
        'div.choices',
        ctrl.data.rating.available.map(r =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.data.rating.selected().includes(r)}`,
              },
              hook: bind('click', _ => ctrl.toggleRating(r), ctrl.redraw),
            },
            r.toString()
          )
        )
      ),
    ]),
    h('section.speed', [
      h('label', ctrl.trans.noarg('timeControl')),
      h(
        'div.choices',
        ctrl.data.speed.available.map(s =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.data.speed.selected().includes(s)}`,
              },
              hook: bind('click', _ => ctrl.toggleSpeed(s), ctrl.redraw),
            },
            s
          )
        )
      ),
    ]),
  ]);
