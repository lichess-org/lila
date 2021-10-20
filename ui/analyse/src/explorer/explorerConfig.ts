import { h, VNode } from 'snabbdom';
import { Prop, prop } from 'common';
import { bind, dataIcon, onInsert } from 'common/snabbdom';
import { storedProp, storedJsonProp, StoredJsonProp, StoredProp } from 'common/storage';
import { ExplorerDb, ExplorerSpeed, ExplorerMode } from './interfaces';
import { snabModal } from 'common/modal';
import AnalyseCtrl from '../ctrl';
import { perf } from 'game/perf';
import { iconTag } from '../util';
import { ucfirst } from './explorerUtil';
import { Color } from 'chessground/types';
import { opposite } from 'chessground/util';

const allDbs: ExplorerDb[] = ['lichess', 'player'];
const allSpeeds: ExplorerSpeed[] = ['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'];
const allModes: ExplorerMode[] = ['casual', 'rated'];
const allRatings = [1600, 1800, 2000, 2200, 2500];

type Month = string;

export interface ExplorerConfigData {
  open: Prop<boolean>;
  db: StoredProp<ExplorerDb>;
  rating: StoredJsonProp<number[]>;
  speed: StoredJsonProp<ExplorerSpeed[]>;
  mode: StoredJsonProp<ExplorerMode[]>;
  since: StoredProp<Month>;
  until: StoredProp<Month>;
  playerName: {
    open: Prop<boolean>;
    value: StoredProp<string>;
    previous: StoredJsonProp<string[]>;
  };
  color: Prop<Color>;
}

export class ExplorerConfigCtrl {
  data: ExplorerConfigData;

  constructor(readonly root: AnalyseCtrl, readonly variant: VariantKey, readonly onClose: () => void) {
    if (variant === 'standard') allDbs.unshift('masters');
    this.data = {
      open: prop(false),
      db: storedProp('explorer.db.' + variant, allDbs[0]),
      rating: storedJsonProp('explorer.rating', () => allRatings),
      speed: storedJsonProp<ExplorerSpeed[]>('explorer.speed', () => allSpeeds),
      mode: storedJsonProp<ExplorerMode[]>('explorer.mode', () => allModes),
      since: storedProp('explorer.since', '2010-01'),
      until: storedProp('explorer.until', ''),
      playerName: {
        open: prop(false),
        value: storedProp<string>('explorer.player.name', document.body.dataset['user'] || ''),
        previous: storedJsonProp<string[]>('explorer.player.name.previous', () => []),
      },
      color: prop('white'),
    };
  }

  toggleMany =
    <T>(c: StoredJsonProp<T[]>) =>
    (value: T) => {
      if (!c().includes(value)) c(c().concat([value]));
      else if (c().length > 1) c(c().filter(v => v !== value));
    };

  toggleOpen = () => {
    this.data.open(!this.data.open());
    if (!this.data.open()) this.onClose();
  };
  fullHouse = () =>
    this.data.db() === 'masters' ||
    (this.data.rating().length === allRatings.length && this.data.speed().length === allSpeeds.length);
}

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  return [
    h('section.db', [
      h('label', ctrl.root.trans.noarg('database')),
      h(
        'div.choices',
        allDbs.map(s =>
          h(
            'button',
            {
              attrs: {
                'aria-pressed': `${ctrl.data.db() === s}`,
              },
              hook: bind('click', _ => ctrl.data.db(s), ctrl.root.redraw),
            },
            s
          )
        )
      ),
    ]),
    ctrl.data.db() === 'masters' ? masterDb(ctrl) : ctrl.data.db() === 'lichess' ? lichessDb(ctrl) : playerDb(ctrl),
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
          hook: bind('click', () => ctrl.data.color(opposite(ctrl.data.color())), ctrl.root.redraw),
        },
        ctrl.data.color()
      ),
      h('strong.beta', 'BETA'),
    ]),
    speedSection(ctrl),
    modeSection(ctrl),
    monthSection(ctrl),
  ]);
};

const masterDb = (ctrl: ExplorerConfigCtrl) =>
  h('div.masters.message', [
    h('i', { attrs: dataIcon('') }),
    h('p', ctrl.root.trans('masterDbExplanation', 2200, '1952', '2019')),
  ]);

const radioButton =
  <T>(ctrl: ExplorerConfigCtrl, storage: StoredJsonProp<T[]>, render?: (t: T) => VNode) =>
  (v: T) =>
    h(
      'button',
      {
        attrs: { 'aria-pressed': `${storage().includes(v)}`, title: render ? ucfirst('' + v) : '' },
        hook: bind('click', _ => ctrl.toggleMany(storage)(v), ctrl.root.redraw),
      },
      render ? render(v) : '' + v
    );

const lichessDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    h('section.rating', [
      h('label', ctrl.root.trans.noarg('averageElo')),
      h('div.choices', allRatings.map(radioButton(ctrl, ctrl.data.rating))),
    ]),
    speedSection(ctrl),
  ]);

const speedSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.speed', [
    h('label', ctrl.root.trans.noarg('timeControl')),
    h('div.choices', allSpeeds.map(radioButton(ctrl, ctrl.data.speed, s => iconTag(perf.icons[s])))),
  ]);

const modeSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.mode', [
    h('label', ctrl.root.trans.noarg('mode')),
    h('div.choices', allModes.map(radioButton(ctrl, ctrl.data.mode))),
  ]);

const monthInput = (prop: StoredProp<Month>) =>
  h('input', {
    attrs: {
      type: 'month',
      pattern: '^20[12][0-9]-(0[1-9]|1[012])$',
      min: '2010-01',
      max: '2030-01',
      value: prop() || '',
    },
    hook: bind('change', e => {
      const input = e.target as HTMLInputElement;
      if (input.checkValidity()) prop(input.value);
    }),
  });

const monthSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.month', [
    h('label', ['Since', monthInput(ctrl.data.since)]),
    h('label', ['Until', monthInput(ctrl.data.until)]),
  ]);

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
            `button.button${name == myName ? '.button-green' : ''}`,
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
