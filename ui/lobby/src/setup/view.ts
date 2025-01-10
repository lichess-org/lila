import { useJp } from 'common/common';
import { initOneWithState, update } from 'common/mini-board';
import { modal } from 'common/modal';
import { getPerfIcon } from 'common/perf-icons';
import { type MaybeVNodes, bind, dataIcon } from 'common/snabbdom';
import { i18n } from 'i18n';
import { i18nPerf } from 'i18n/perf';
import { colorName } from 'shogi/color-name';
import { findHandicaps, isHandicap } from 'shogiops/handicaps';
import { type VNode, h } from 'snabbdom';
import type SetupCtrl from './ctrl';
import {
  Position,
  aiLevelChoices,
  byoChoices,
  colorChoices,
  dayChoices,
  fieldId,
  formatMinutes,
  incChoices,
  maxRatingChoices,
  minRatingChoices,
  modeChoicesTranslated,
  periodChoices,
  positionChoicesTranslated,
  radioGroup,
  select,
  selectNvui,
  slider,
  timeChoices,
  timeModeChoicesTranslated,
  variantChoicesTranslated,
} from './util';

export function setupModal(ctrl: SetupCtrl): VNode {
  return modal({
    class: 'lobby-setup',
    content: ctrl.nvui ? innerModalNvui(ctrl) : innerModal(ctrl),
    onClose() {
      ctrl.close();
    },
  });
}

function innerModal(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', i18n('createAGame')),
    variant(ctrl),
    ctrl.key !== 'hook' ? position(ctrl) : undefined,
    timeControl(ctrl),
    ctrl.key !== 'ai' ? mode(ctrl) : undefined,
    ctrl.key === 'ai' ? levels(ctrl) : undefined,
    submitButtons(ctrl),
    rating(ctrl),
  ];
}

function variant(ctrl: SetupCtrl): VNode {
  return h('div.setup-variant.section.select', [
    h('a.info', {
      attrs: { href: '/variant', target: '_blank', 'data-icon': '', title: i18n('variants') },
    }),
    h('label', {}, i18n('variant') + ':'),
    select(ctrl, 'variant', variantChoicesTranslated),
  ]);
}

function position(ctrl: SetupCtrl): VNode {
  return h('div.setup-position.section', [
    radioGroup(ctrl, 'position', positionChoicesTranslated),
    ctrl.data.position == Position.fromPosition ? positionInput(ctrl) : undefined,
  ]);
}

function positionInput(ctrl: SetupCtrl): VNode {
  const jp = useJp(),
    handicapLink = jp
      ? 'https://ja.wikipedia.org/wiki/%E5%B0%86%E6%A3%8B%E3%81%AE%E6%89%8B%E5%90%88%E5%89%B2'
      : 'https://en.wikipedia.org/wiki/Handicap_(shogi)',
    variant = ctrl.variantKey();

  return h('div.setup-position-input', [
    h('div.setup-handicap.select', [
      h('a.info', {
        attrs: { href: handicapLink, target: '_blank', 'data-icon': '', title: i18n('variants') },
      }),
      h('label', { attrs: { for: 'handicap' } }, i18n('handicap') + '?' + ':'),
      select(
        ctrl,
        'handicap',
        findHandicaps({ rules: variant }).map(h => {
          const name = jp ? h.japaneseName : h.englishName;
          return [h.sfen, name];
        }),
        true,
      ),
    ]),
    h('div.setup-sfen', [
      h('a.info', {
        attrs: { href: '/editor', target: '_blank', 'data-icon': 'm', title: i18n('boardEditor') },
      }),
      sfenInput(ctrl),
    ]),
    h(
      'div.setup-position-board',
      h(`div.sg-wrap.mini-board.v-${ctrl.variantKey()}${!ctrl.data.sfen ? '.none' : ''}`, {
        key: ctrl.data.sfen,
        hook: {
          insert: vnode => {
            initOneWithState(vnode.elm as HTMLElement, {
              sfen: ctrl.data.sfen,
              orientation: isHandicap({ sfen: ctrl.data.sfen }) ? 'gote' : 'sente',
              variant: ctrl.variantKey(),
            });
          },
          update: vnode => {
            update(vnode.elm as HTMLElement, ctrl.data.sfen);
          },
        },
      }),
    ),
  ]);
}

function sfenInput(ctrl: SetupCtrl): VNode {
  return h('input', {
    class: {
      success: !ctrl.invalidSfen,
      failure: ctrl.invalidSfen,
    },
    hook: bind('keyup', e => {
      ctrl.set('sfen', (e.target as HTMLSelectElement).value);
    }),
    attrs: {
      id: fieldId('sfen'),
      type: 'text',
      name: 'sfen',
      placeholder: 'SFEN',
      value: ctrl.data.sfen,
    },
  });
}

function timeControl(ctrl: SetupCtrl): VNode {
  return h('div.setup-time.section', [
    h('div.setup-time-mode', [radioGroup(ctrl, 'timeMode', timeModeChoicesTranslated)]),
    ...(ctrl.isCorres() ? timeControlCorres(ctrl) : timeControlRt(ctrl)),
  ]);
}

function timeControlRt(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('div.setup-time-rt', [
      h('div.label', [i18n('minutesPerSide'), ': ', h('strong', formatMinutes(ctrl.data.time!))]),
      slider(ctrl, 'time', timeChoices, true),
    ]),
    h('div.setup-time-byo', [
      h('div.label', [i18n('byoyomiInSeconds'), ': ', h('strong', ctrl.data.byoyomi)]),
      slider(ctrl, 'byoyomi', byoChoices, true),
    ]),
    h(`div.setup-time-extra-toggle${ctrl.isExtraOpen ? '.open' : ''}`, {
      attrs: {
        'data-icon': 'u',
      },
      hook: bind('click', () => {
        ctrl.toggleExtra();
      }),
    }),
    ctrl.isExtraOpen ? timeControlExtra(ctrl) : undefined,
  ];
}

function timeControlCorres(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('div.setup-time-corres', [
      h('div.setup-time-days', [
        h('div.label', [i18n('daysPerTurn'), ': ', h('strong', ctrl.data.days)]),
        slider(ctrl, 'days', dayChoices, true),
      ]),
    ]),
  ];
}

function timeControlExtra(ctrl: SetupCtrl): VNode {
  return h('div.setup-time-extra', [
    h('div.label', i18n('periods')),
    radioGroup(
      ctrl,
      'periods',
      periodChoices.map(n => [n, `${n}`]),
    ),
    h('div.setup-time-extra-inc', [
      h('div.label', [i18n('incrementInSeconds'), ': ', h('strong', ctrl.data.increment)]),
      slider(ctrl, 'increment', incChoices, true),
    ]),
  ]);
}

function mode(ctrl: SetupCtrl): VNode {
  return h('div.setup-mode.section', [
    radioGroup(ctrl, 'mode', modeChoicesTranslated, ctrl.canBeRated() ? undefined : 1),
    ctrl.key === 'hook'
      ? h('div.setup-rating-range', [
          h('div.label', [i18n('ratingRange')]),
          h('div.small-sliders', [
            slider(ctrl, 'ratingMin', minRatingChoices),
            h('div.rating-values', [
              h('strong.right', `-${ctrl.data.ratingMin}`),
              ' / ',
              h('strong', `+${ctrl.data.ratingMax}`),
            ]),
            slider(ctrl, 'ratingMax', maxRatingChoices),
          ]),
        ])
      : undefined,
  ]);
}

function levels(ctrl: SetupCtrl): VNode {
  return h('div.setup-levels.section', [
    h('div.label', ctrl.engineName()),
    radioGroup(
      ctrl,
      'level',
      aiLevelChoices.map(l => [l, `${l}`]),
    ),
  ]);
}

function submitButtons(ctrl: SetupCtrl): VNode {
  const allDisabled = !ctrl.canSubmit();
  return h(
    'div.setup-submits.section',
    colorChoices.map(color => {
      const name = color === 'random' ? i18n('randomColor') : colorName(color, ctrl.isHandicap()),
        disabled = allDisabled || (color !== 'random' && !ctrl.canChooseColor());
      return h('div.button-wrap', [
        h(`button.button.button-metal.color-icon.${color}${disabled ? '.disabled' : ''}`, {
          attrs: {
            title: name,
            disabled,
          },
          hook: bind('click', () => {
            ctrl.submit(color);
          }),
        }),
        h('div.button-title', name),
      ]);
    }),
  );
}

function rating(ctrl: SetupCtrl): VNode {
  const p = ctrl.perf();
  return h(
    'div.setup-ratings',
    h(
      'div.setup-ratings-value',
      p
        ? [
            h(
              'span.text',
              {
                attrs: dataIcon(getPerfIcon(p)),
              },
              i18nPerf(p),
            ),
            h('strong', ctrl.rating()),
          ]
        : [],
    ),
  );
}

function innerModalNvui(ctrl: SetupCtrl): MaybeVNodes {
  let color: Color | 'random' = 'random';
  return [
    h('h2', i18n('createAGame')),
    selectNvui(ctrl, i18n('variant'), 'variant', variantChoicesTranslated),
    ctrl.key !== 'hook'
      ? h('div', [
          selectNvui(ctrl, i18n('fromPosition'), 'position', positionChoicesTranslated),
          ctrl.data.position == Position.fromPosition
            ? h('div', [h('label', { attrs: { for: fieldId('sfen') } }, 'SFEN'), sfenInput(ctrl)])
            : undefined,
        ])
      : undefined,
    selectNvui(ctrl, i18n('timeControl'), 'timeMode', timeModeChoicesTranslated),
    ctrl.isCorres()
      ? h('div', [selectNvui(ctrl, i18n('daysPerTurn'), 'days', dayChoices)])
      : h('div', [
          selectNvui(ctrl, i18n('minutesPerSide'), 'time', timeChoices),
          selectNvui(ctrl, i18n('byoyomiInSeconds'), 'byoyomi', byoChoices),
          selectNvui(ctrl, i18n('periods'), 'periods', periodChoices),
          selectNvui(ctrl, i18n('incrementInSeconds'), 'increment', incChoices),
        ]),
    ctrl.key !== 'ai' ? selectNvui(ctrl, i18n('mode'), 'mode', modeChoicesTranslated) : undefined,
    ctrl.key === 'ai' ? selectNvui(ctrl, i18n('level'), 'level', aiLevelChoices) : undefined,
    h('div.color-submit', [
      h('label', { attrs: { for: `ls-color-submit` } }, i18n('side')),
      h(
        'select',
        {
          attrs: {
            id: `ls-color-submit`,
            name: 'color',
          },
          hook: bind('change', e => {
            color = (e.target as HTMLSelectElement).value as Color | 'random';
          }),
        },
        colorChoices.map(c =>
          h(
            'option',
            { attrs: { value: c, selected: c === 'random' } },
            c === 'random' ? i18n('randomColor') : colorName(c, ctrl.isHandicap()),
          ),
        ),
      ),
      h(
        'button',
        {
          attrs: { type: 'submit' },
          hook: bind('click', () => {
            ctrl.submit(color);
          }),
        },
        i18n('createAGame'),
      ),
    ]),
    rating(ctrl),
  ];
}
