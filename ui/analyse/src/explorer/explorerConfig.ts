import { h, VNode } from 'snabbdom';
import { Prop, prop } from 'common';
import { bind, dataIcon, onInsert } from 'common/snabbdom';
import { storedProp, storedJsonProp, StoredJsonProp, StoredProp } from 'common/storage';
import { ExplorerDb, ExplorerSpeed, ExplorerMode } from './interfaces';
import { snabModal } from 'common/modal';
import AnalyseCtrl from '../ctrl';

const allSpeeds: ExplorerSpeed[] = ['bullet', 'blitz', 'rapid', 'classical'];
const allModes: ExplorerMode[] = ['casual', 'rated'];
const allRatings = [1600, 1800, 2000, 2200, 2500];

export interface ExplorerConfigData {
  open: Prop<boolean>;
  db: {
    available: ExplorerDb[];
    selected: StoredProp<ExplorerDb>;
  };
  rating: {
    available: number[];
    selected: StoredJsonProp<number[]>;
  };
  speed: {
    available: ExplorerSpeed[];
    selected: StoredJsonProp<ExplorerSpeed[]>;
  };
  mode: {
    available: ExplorerMode[];
    selected: StoredJsonProp<ExplorerMode[]>;
  };
  playerName: {
    open: Prop<boolean>;
    value: StoredProp<string>;
    previous: StoredJsonProp<string[]>;
  };
}

export class ExplorerConfigCtrl {
  data: ExplorerConfigData;

  constructor(readonly root: AnalyseCtrl, readonly variant: VariantKey, readonly onClose: () => void) {
    const available: ExplorerDb[] = ['lichess', 'player'];
    if (variant === 'standard') available.unshift('masters');
    this.data = {
      open: prop(true),
      db: {
        available,
        selected: available.length > 1 ? storedProp('explorer.db.' + variant, available[0]) : () => available[0],
      },
      rating: {
        available: allRatings,
        selected: storedJsonProp('explorer.rating', () => allRatings),
      },
      speed: {
        available: allSpeeds,
        selected: storedJsonProp<ExplorerSpeed[]>('explorer.speed', () => allSpeeds),
      },
      mode: {
        available: allModes,
        selected: storedJsonProp<ExplorerMode[]>('explorer.mode', () => allModes),
      },
      playerName: {
        open: prop(false),
        value: storedProp<string>('explorer.player.name', document.body.dataset['user'] || ''),
        previous: storedJsonProp<string[]>('explorer.player.name.previous', () => []),
      },
    };
  }

  db = () => this.data.db.selected();

  toggleMany = <T>(c: StoredJsonProp<T[]>, value: T) => {
    if (!c().includes(value)) c(c().concat([value]));
    else if (c().length > 1) c(c().filter(v => v !== value));
  };

  toggleOpen = () => {
    this.data.open(!this.data.open());
    if (!this.data.open()) this.onClose();
  };
  toggleRating = (v: number) => this.toggleMany(this.data.rating.selected, v);
  toggleSpeed = (v: ExplorerSpeed) => this.toggleMany(this.data.speed.selected, v);
  toggleMode = (v: ExplorerMode) => this.toggleMany(this.data.mode.selected, v);
  fullHouse = () =>
    this.db() === 'masters' ||
    (this.data.rating.selected().length === this.data.rating.available.length &&
      this.data.speed.selected().length === this.data.speed.available.length);
}

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  return [
    h('section.db', [
      h('label', ctrl.root.trans.noarg('database')),
      h(
        'div.choices',
        ctrl.data.db.available.map(s =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.db() === s}`,
              },
              hook: bind('click', _ => ctrl.data.db.selected(s), ctrl.root.redraw),
            },
            s
          )
        )
      ),
    ]),
    ctrl.db() === 'masters' ? masterDb(ctrl) : ctrl.db() === 'lichess' ? lichessDb(ctrl) : playerDb(ctrl),
    h(
      'section.save',
      h(
        'button.button.button-green.text',
        {
          attrs: dataIcon(''),
          hook: bind('click', ctrl.toggleOpen),
        },
        ctrl.root.trans.noarg('allSet')
      )
    ),
  ];
}

const playerDb = (ctrl: ExplorerConfigCtrl) => {
  const name = ctrl.data.playerName.value();
  return h('div.player-db', [
    ctrl.data.playerName.open() ? playerModal(ctrl) : undefined,
    h('section.name', [
      h(
        'button.button.player-name',
        {
          hook: bind('click', () => ctrl.data.playerName.open(true), ctrl.root.redraw),
        },
        name || 'Select a Lichess player'
      ),
      ' as ',
      h(
        'button.button-link.text.color',
        {
          attrs: {
            ...dataIcon(''),
            title: ctrl.root.trans('flipBoard'),
          },
          hook: bind('click', ctrl.root.flip),
        },
        ctrl.root.getOrientation()
      ),
    ]),
    speedSection(ctrl),
    modeSection(ctrl),
  ]);
};

const playerModal = (ctrl: ExplorerConfigCtrl) => {
  const myName = document.body.dataset['user'];
  const onSelect = (name: string) => {
    if (name != myName) {
      const previous = ctrl.data.playerName.previous().filter(n => n !== name);
      previous.unshift(name);
      ctrl.data.playerName.previous(previous.slice(0, 20));
    }
    ctrl.data.playerName.value(name);
    ctrl.data.playerName.open(false);
    ctrl.root.redraw();
  };
  return snabModal({
    class: 'explorer__config__player__choice',
    onClose() {
      ctrl.data.playerName.open(false);
      ctrl.root.redraw();
    },
    content: [
      h('h2', 'Personal opening explorer'),
      h('div.input-wrapper', [
        h('input', {
          attrs: { placeholder: ctrl.root.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(input =>
            lichess.userComplete().then(uac => {
              uac({
                input,
                tag: 'span',
                onSelect: v => onSelect(v.name),
              });
              input.focus();
            })
          ),
        }),
      ]),
      h(
        'div.previous',
        [...(myName ? [myName] : []), ...ctrl.data.playerName.previous()].map(name =>
          h(
            'button.button',
            {
              hook: bind('click', () => onSelect(name)),
            },
            name
          )
        )
      ),
    ],
  });
};

const masterDb = (ctrl: ExplorerConfigCtrl) =>
  h('div.masters.message', [
    h('i', { attrs: dataIcon('') }),
    h('p', ctrl.root.trans('masterDbExplanation', 2200, '1952', '2019')),
  ]);

const lichessDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    h('section.rating', [
      h('label', ctrl.root.trans.noarg('averageElo')),
      h(
        'div.choices',
        ctrl.data.rating.available.map(r =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.data.rating.selected().includes(r)}`,
              },
              hook: bind('click', _ => ctrl.toggleRating(r), ctrl.root.redraw),
            },
            r.toString()
          )
        )
      ),
    ]),
    speedSection(ctrl),
  ]);

const speedSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.speed', [
    h('label', ctrl.root.trans.noarg('timeControl')),
    h(
      'div.choices',
      ctrl.data.speed.available.map(s =>
        h(
          'button',
          {
            attrs: {
              'aria-pressed': `${ctrl.data.speed.selected().includes(s)}`,
            },
            hook: bind('click', _ => ctrl.toggleSpeed(s), ctrl.root.redraw),
          },
          s
        )
      )
    ),
  ]);

const modeSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.mode', [
    h('label', ctrl.root.trans.noarg('mode')),
    h(
      'div.choices',
      ctrl.data.mode.available.map(s =>
        h(
          'button',
          {
            attrs: {
              'aria-pressed': `${ctrl.data.mode.selected().includes(s)}`,
            },
            hook: bind('click', _ => ctrl.toggleMode(s), ctrl.root.redraw),
          },
          s
        )
      )
    ),
  ]);
