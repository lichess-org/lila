import { h, type VNode } from 'snabbdom';
import { myUsername, type Prop, prop } from 'lib';
import * as licon from 'lib/licon';
import { type Dialog, snabDialog } from 'lib/view/dialog';
import { bind, dataIcon, iconTag, onInsert } from 'lib/snabbdom';
import { storedProp, storedJsonProp, type StoredProp, storedStringProp } from 'lib/storage';
import type { ExplorerDb, ExplorerSpeed, ExplorerMode } from './interfaces';
import AnalyseCtrl from '../ctrl';
import perfIcons from 'lib/game/perfIcons';
import { ucfirst } from './explorerUtil';
import { opposite } from '@lichess-org/chessground/util';
import { userComplete } from 'lib/view/userComplete';

const allSpeeds: ExplorerSpeed[] = ['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'];
const allModes: ExplorerMode[] = ['casual', 'rated'];
const allRatings = [400, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2500];
const minYear = 1952;

type Month = string;
type ByDbSetting = {
  since: StoredProp<Month>;
  until: StoredProp<Month>;
};
type ByDbSettings = {
  [key in ExplorerDb]: ByDbSetting;
};

export interface ExplorerConfigData {
  open: Prop<boolean>;
  db: StoredProp<ExplorerDb>;
  rating: Prop<number[]>;
  speed: Prop<ExplorerSpeed[]>;
  mode: Prop<ExplorerMode[]>;
  byDbData: ByDbSettings;
  playerName: {
    open: Prop<boolean>;
    value: StoredProp<string>;
    previous: Prop<string[]>;
  };
  color: Prop<Color>;
  byDb(): ByDbSetting;
}

export class ExplorerConfigCtrl {
  data: ExplorerConfigData;
  allDbs: ExplorerDb[] = ['lichess', 'player'];
  myName?: string;
  participants: (string | undefined)[];

  constructor(
    readonly root: AnalyseCtrl,
    readonly variant: VariantKey,
    readonly onClose: () => void,
    previous?: ExplorerConfigCtrl,
  ) {
    this.myName = myUsername();
    this.participants = [root.data.player.user?.username, root.data.opponent.user?.username].filter(
      name => name && name !== this.myName,
    );
    if (variant === 'standard') this.allDbs.unshift('masters');
    const byDbData = {} as ByDbSettings;
    for (const db of this.allDbs) {
      byDbData[db] = {
        since: storedStringProp('analyse.explorer.since-2.' + db, ''),
        until: storedStringProp('analyse.explorer.until-2.' + db, ''),
      };
    }
    const prevData = previous?.data;
    this.data = {
      open: prevData?.open || prop(false),
      db: storedProp<ExplorerDb>(
        'explorer.db2.' + variant,
        this.allDbs[0],
        str => str as ExplorerDb,
        v => v,
      ),
      rating: storedJsonProp('analyse.explorer.rating', () => allRatings.slice(1)),
      speed: storedJsonProp<ExplorerSpeed[]>('explorer.speed', () => allSpeeds.slice(1)),
      mode: storedJsonProp<ExplorerMode[]>('explorer.mode', () => allModes),
      byDbData,
      playerName: {
        open: prevData?.playerName.open || prop(false),
        value: storedStringProp('analyse.explorer.player.name', this.myName || ''),
        previous: storedJsonProp<string[]>('explorer.player.name.previous', () => []),
      },
      color: prevData?.color || prop(root.bottomColor()),
      byDb() {
        return this.byDbData[this.db()] || this.byDbData.lichess;
      },
    };
  }

  selectPlayer = (name?: string) => {
    name = name === 'me' ? this.myName : name;
    if (!name) return;
    if (name === name.toLowerCase()) {
      /* If the user provides a lowercase version of a username that already
         exists in the personal opening explorer, don't make a new button and instead
         just use the existing one. */
      name =
        [
          ...this.data.playerName.previous(),
          this.data.playerName.value(),
          this.myName,
          ...this.participants,
        ].find(x => x?.toLowerCase() === name!.toLowerCase()) ?? name;
    }
    if (name !== this.myName && !this.participants.includes(name)) {
      const previous = this.data.playerName.previous().filter(n => n !== name);
      previous.unshift(name);
      this.data.playerName.previous(previous.slice(0, 20));
    }
    this.data.db('player');
    this.data.playerName.value(name);
  };

  removePlayer = (name?: string) => {
    if (!name) return;
    const previous = this.data.playerName.previous().filter(n => n !== name);
    this.data.playerName.previous(previous);

    if (this.data.playerName.value() === name) this.data.playerName.value('');
  };

  toggleMany =
    <T>(c: Prop<T[]>) =>
    (value: T) => {
      if (!c().includes(value)) c(c().concat([value]));
      else if (c().length > 1) c(c().filter(v => v !== value));
    };

  toggleColor = () => this.data.color(opposite(this.data.color()));

  toggleOpen = () => {
    this.data.open(!this.data.open());
    if (!this.data.open()) {
      if (this.data.db() === 'player' && !this.data.playerName.value()) this.data.db('lichess');
      this.onClose();
    }
  };

  fullHouse = () =>
    this.data.byDb().since() <= `${minYear}-01` &&
    (!this.data.byDb().until() || new Date().toISOString().slice(0, 7) <= this.data.byDb().until()) &&
    (this.data.db() === 'masters' || this.data.speed().length === allSpeeds.length) &&
    (this.data.db() !== 'lichess' || this.data.rating().length === allRatings.length) &&
    (this.data.db() !== 'player' || this.data.mode().length === allModes.length);
}

export function view(ctrl: ExplorerConfigCtrl): VNode[] {
  return [
    ctrl.data.db() === 'masters'
      ? masterDb(ctrl)
      : ctrl.data.db() === 'lichess'
        ? lichessDb(ctrl)
        : playerDb(ctrl),
    h(
      'section.save',
      h(
        'button.button.button-green.text',
        { attrs: dataIcon(licon.Checkmark), hook: bind('click', ctrl.toggleOpen) },
        i18n.site.allSet,
      ),
    ),
  ];
}

const selectText = 'Select a Lichess player';

const playerDb = (ctrl: ExplorerConfigCtrl) => {
  const name = ctrl.data.playerName.value();
  return h('div.player-db', [
    ctrl.data.playerName.open() ? playerModal(ctrl) : undefined,
    h('section.name', [
      h('label', i18n.site.player),
      h('div', [
        h(
          'div.choices',
          h(
            `button.player-name${name ? '.active' : ''}`,
            {
              hook: bind('click', () => ctrl.data.playerName.open(true), ctrl.root.redraw),
              attrs: name ? { title: selectText } : undefined,
            },
            name || selectText,
          ),
        ),
        h(
          'button.button-link.text.color',
          {
            attrs: dataIcon(licon.ChasingArrows),
            hook: bind('click', ctrl.toggleColor, ctrl.root.redraw),
          },
          ` ${i18n.site[ctrl.data.color() === 'white' ? 'asWhite' : 'asBlack']}`,
        ),
      ]),
    ]),
    speedSection(ctrl),
    modeSection(ctrl),
    monthSection(ctrl),
  ]);
};

const masterDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    h('section.date', [
      h('label', [i18n.site.since, yearInput(ctrl.data.byDb().since, () => '', ctrl.root.redraw)]),
      h('label', [
        i18n.site.until,
        yearInput(ctrl.data.byDb().until, ctrl.data.byDb().since, ctrl.root.redraw),
      ]),
    ]),
  ]);

const radioButton =
  <T>(ctrl: ExplorerConfigCtrl, storage: Prop<T[]>, render?: (t: T) => VNode) =>
  (v: T) =>
    h(
      'button',
      {
        attrs: { 'aria-pressed': `${storage().includes(v)}`, title: render ? ucfirst('' + v) : '' },
        hook: bind('click', _ => ctrl.toggleMany(storage)(v), ctrl.root.redraw),
      },
      render ? render(v) : i18n(v as string),
    );

const lichessDb = (ctrl: ExplorerConfigCtrl) =>
  h('div', [
    speedSection(ctrl),
    h('section.rating', [
      h('label', i18n.site.averageElo),
      h('div.choices', allRatings.map(radioButton(ctrl, ctrl.data.rating))),
    ]),
    monthSection(ctrl),
  ]);

const speedSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.speed', [
    h('label', i18n.site.timeControl),
    h('div.choices', allSpeeds.map(radioButton(ctrl, ctrl.data.speed, s => iconTag(perfIcons[s])))),
  ]);

const modeSection = (ctrl: ExplorerConfigCtrl) =>
  h('section.mode', [
    h('label', i18n.site.mode),
    h('div.choices', allModes.map(radioButton(ctrl, ctrl.data.mode))),
  ]);

const monthInput = (prop: StoredProp<Month>, after: () => Month, redraw: Redraw) => {
  const validateRange = (input: HTMLInputElement) =>
    input.setCustomValidity(!input.value || after() <= input.value ? '' : 'Invalid date range');
  const max = new Date().toISOString().slice(0, 7);
  return h('input', {
    key: after() ? 'until-month' : 'since-month',
    attrs: {
      type: 'month',
      title: `Insert year and month in YYYY-MM format starting from ${minYear}-01`,
      pattern: '^(19|20)[0-9]{2}-(0[1-9]|1[012])$',
      placeholder: 'YYYY-MM',
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
      key: after() ? 'until-year' : 'since-year',
      type: 'number',
      title: `Insert year in YYYY format starting from ${minYear}`,
      placeholder: 'YYYY',
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
            prop(input.value ? `${input.value}-${after() ? '12' : '01'}` : '');
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
    h('label', [i18n.site.since, monthInput(ctrl.data.byDb().since, () => '', ctrl.root.redraw)]),
    h('label', [
      i18n.site.until,
      monthInput(ctrl.data.byDb().until, ctrl.data.byDb().since, ctrl.root.redraw),
    ]),
  ]);

const playerModal = (ctrl: ExplorerConfigCtrl) => {
  let dlg: Dialog;
  const onSelect = (name: string | undefined) => {
    ctrl.selectPlayer(name);
    dlg.close();
  };
  const nameToOptionalColor = (name: string | undefined) => {
    if (!name) return;
    else if (name === ctrl.myName) return '.button-green';
    else if (ctrl.data.playerName.previous().includes(name)) return '';
    return '.button-metal';
  };
  return snabDialog({
    class: 'explorer__config__player__choice',
    onClose() {
      ctrl.data.playerName.open(false);
      ctrl.root.redraw();
    },
    onInsert: dialog => (dlg = dialog).show(),
    modal: true,
    vnodes: [
      h('h2', 'Personal opening explorer'),
      h('div.input-wrapper', [
        h('input', {
          attrs: {
            placeholder: i18n.study.searchByUsername,
            spellcheck: 'false',
          },
          hook: onInsert<HTMLInputElement>(input =>
            userComplete({ input, focus: true, tag: 'span', onSelect: v => onSelect(v.name) }),
          ),
        }),
      ]),
      h(
        'div.previous',
        [
          ...new Set([
            ...(ctrl.myName ? [ctrl.myName] : []),
            ...ctrl.participants,
            ...ctrl.data.playerName.previous(),
          ]),
        ].map(name =>
          h('div', { key: name }, [
            h(
              `button.button${nameToOptionalColor(name)}`,
              { hook: bind('click', () => onSelect(name)) },
              name,
            ),
            name && ctrl.data.playerName.previous().includes(name)
              ? h('button.remove', {
                  attrs: dataIcon(licon.X),
                  hook: bind('click', () => ctrl.removePlayer(name), ctrl.root.redraw),
                })
              : null,
          ]),
        ),
      ),
    ],
  });
};
