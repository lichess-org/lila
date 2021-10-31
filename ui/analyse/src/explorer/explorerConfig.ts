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
import { Redraw } from '../interfaces';

const allSpeeds: ExplorerSpeed[] = ['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'];
const allModes: ExplorerMode[] = ['casual', 'rated'];
const allRatings = [1600, 1800, 2000, 2200, 2500];
const minYear = 1952;

type Month = string;

export interface ExplorerConfigData {
  open: Prop<boolean>;
  advanced: StoredJsonProp<boolean>;
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
  allDbs: ExplorerDb[] = ['lichess', 'player'];
  myName?: string;

  constructor(readonly root: AnalyseCtrl, readonly variant: VariantKey, readonly onClose: () => void) {
    this.myName = document.body.dataset['user'];
    if (variant === 'standard') this.allDbs.unshift('masters');
    this.data = {
      open: prop(false),
      advanced: storedJsonProp('explorer.advanced', () => false),
      db: storedProp('explorer.db.' + variant, this.allDbs[0]),
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

  selectPlayer = (name?: string) => {
    name = name == 'me' ? this.myName : name;
    if (!name) return;
    if (name != this.myName) {
      const previous = this.data.playerName.previous().filter(n => n !== name);
      previous.unshift(name);
      this.data.playerName.previous(previous.slice(0, 20));
    }
    this.data.db('player');
    this.data.playerName.value(name);
    this.data.playerName.open(false);
  };

  toggleMany =
    <T>(c: StoredJsonProp<T[]>) =>
    (value: T) => {
      if (!c().includes(value)) c(c().concat([value]));
      else if (c().length > 1) c(c().filter(v => v !== value));
    };

  toggleColor = () => this.data.color(opposite(this.data.color()));

  toggleOpen = () => {
    this.data.open(!this.data.open());
    if (!this.data.open()) {
      if (this.data.db() == 'player' && !this.data.playerName.value()) this.data.db('lichess');
      this.onClose();
    }
  };

  fullHouse = () =>
    this.data.since() <= `${minYear}-01` &&
    (!this.data.until() || new Date().toISOString().slice(0, 7) <= this.data.until()) &&
    (this.data.db() === 'masters' || this.data.speed().length == allSpeeds.length) &&
    (this.data.db() !== 'lichess' || this.data.rating().length == allRatings.length) &&
    (this.data.db() !== 'player' || this.data.mode().length == allModes.length);
}

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  return [
    h('section.db', [
      h('label', ctrl.root.trans.noarg('database')),
      h(
        'div.choices',
        ctrl.allDbs.map(s =>
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

const selectText = 'Select a Lichess player';

const playerDb = (ctrl: ExplorerConfigCtrl) => {
  const name = ctrl.data.playerName.value();
  return h('div.player-db', [
    ctrl.data.playerName.open() ? playerModal(ctrl) : undefined,
    h('section.name', [
      h(
        'div.choices',
        h(
          `button.player-name${name ? '.active' : ''}`,
          {
            hook: bind('click', () => ctrl.data.playerName.open(true), ctrl.root.redraw),
            attrs: name ? { title: selectText } : undefined,
          },
          name || selectText
        )
      ),
      ' as ',
      h(
        'button.button-link.text.color',
        {
          attrs: dataIcon(''),
          hook: bind('click', ctrl.toggleColor, ctrl.root.redraw),
        },
        ctrl.data.color()
      ),
    ]),
    speedSection(ctrl),
    advancedSection(ctrl, [modeSection(ctrl), monthSection(ctrl)]),
  ]);
};

const advancedSection = (ctrl: ExplorerConfigCtrl, content: VNode[]): VNode =>
  h('div.advanced', [
    h(
      'button.button-link.toggle',
      {
        hook: bind('click', () => ctrl.data.advanced(!ctrl.data.advanced()), ctrl.root.redraw),
      },
      ['Advanced settings ', iconTag(ctrl.data.advanced() ? '' : '')]
    ),
    ...(ctrl.data.advanced() ? content : []),
  ]);

const masterDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    h('p.message', ctrl.root.trans('masterDbExplanation', 2200, minYear, '2019')),
    advancedSection(ctrl, [
      h('section.date', [
        h('label', ['Since', yearInput(ctrl.data.since, () => '', ctrl.root.redraw)]),
        h('label', ['Until', yearInput(ctrl.data.until, ctrl.data.since, ctrl.root.redraw)]),
      ]),
    ]),
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
    h('p.message', 'Rated games sampled from all Lichess players'),
    speedSection(ctrl),
    advancedSection(ctrl, [
      h('section.rating', [
        h('label', ctrl.root.trans.noarg('averageElo')),
        h('div.choices', allRatings.map(radioButton(ctrl, ctrl.data.rating))),
      ]),
      monthSection(ctrl),
    ]),
  ]);

const speedSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.speed', [
    h('label', ctrl.root.trans.noarg('timeControl')),
    h('div.choices', allSpeeds.map(radioButton(ctrl, ctrl.data.speed, s => iconTag(perf.icons[s])))),
  ]);

const modeSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.mode', [h('div.choices', allModes.map(radioButton(ctrl, ctrl.data.mode)))]);

const monthInput = (prop: StoredProp<Month>, after: () => Month, redraw: Redraw) => {
  const validateRange = (input: HTMLInputElement) =>
    input.setCustomValidity(!input.value || after() <= input.value ? '' : 'Invalid date range');
  const max = new Date().toISOString().slice(0, 7);
  return h('input', {
    attrs: {
      type: 'month',
      pattern: '^(19|20)[0-9]{2}-(0[1-9]|1[012])$',
      min: `${minYear}-01`,
      max,
      value: prop() > max ? max : prop(),
    },
    hook: {
      insert: vnode => {
        const input = vnode.elm as HTMLInputElement;
        validateRange(input);
        input.addEventListener('change', e => {
          const input = e.target as HTMLInputElement;
          input.setCustomValidity('');
          if (input.checkValidity()) {
            validateRange(input);
            prop(input.value);
            redraw();
          }
        });
      },
      update: (_, vnode) => validateRange(vnode.elm as HTMLInputElement),
    },
  });
};

const yearInput = (prop: StoredProp<Month>, after: () => Month, redraw: Redraw) => {
  const validateRange = (input: HTMLInputElement) =>
    input.setCustomValidity(!input.value || after().split('-')[0] <= input.value ? '' : 'Invalid date range');
  return h('input', {
    attrs: {
      type: 'number',
      min: minYear,
      max: new Date().toISOString().slice(0, 4),
      value: prop().split('-')[0],
    },
    hook: {
      insert: vnode => {
        const input = vnode.elm as HTMLInputElement;
        validateRange(input);
        input.addEventListener('change', e => {
          const input = e.target as HTMLInputElement;
          input.setCustomValidity('');
          if (input.checkValidity()) {
            validateRange(input);
            prop(`${input.value}-${after() ? '12' : '01'}`);
            redraw();
          }
        });
      },
      update: (_, vnode) => validateRange(vnode.elm as HTMLInputElement),
    },
  });
};

const monthSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.date', [
    h('label', ['Since', monthInput(ctrl.data.since, () => '', ctrl.root.redraw)]),
    h('label', ['Until', monthInput(ctrl.data.until, ctrl.data.since, ctrl.root.redraw)]),
  ]);

const playerModal = (ctrl: ExplorerConfigCtrl) => {
  const onSelect = (name: string) => {
    ctrl.selectPlayer(name);
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
        [...(ctrl.myName ? [ctrl.myName] : []), ...ctrl.data.playerName.previous()].map(name =>
          h(
            `button.button${name == ctrl.myName ? '.button-green' : ''}`,
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
