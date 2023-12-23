import { h } from 'snabbdom';
import { Prop } from 'common';
import { bind } from 'common/snabbdom';
import { rangeConfig } from 'common/controls';

export function settingNodes(
  colors: Prop<boolean>,
  clarity: Prop<number>,
  timer: Prop<number>,
  redraw: () => void,
) {
  return [colorsSetting(colors, redraw), claritySetting(clarity, redraw), timerSetting(timer, redraw)];
}

export function colorsSetting(colors: Prop<boolean>, redraw: () => void) {
  return h('div.voice-choices', [
    'Label with',
    h(
      'span.btn-rack',
      ['Colors', 'Numbers'].map(pref =>
        h(
          `span.btn-rack__btn`,
          {
            class: { active: colors() === (pref === 'Colors') },
            hook: bind('click', () => colors(pref === 'Colors'), redraw),
          },
          pref,
        ),
      ),
    ),
  ]);
}

export function claritySetting(clarity: Prop<number>, redraw: () => void) {
  return h('div.voice-setting', [
    h('label', { attrs: { for: 'voice-clarity' } }, 'Clarity'),
    h('input#voice-clarity', {
      attrs: { type: 'range', min: 0, max: 2, step: 1 },
      hook: rangeConfig(clarity, val => {
        clarity(val);
        redraw();
      }),
    }),
    h('div.range_value', ['Fuzzy', 'Average', 'Clear'][clarity()]),
  ]);
}

export function timerSetting(timer: Prop<number>, redraw: () => void) {
  return h('div.voice-setting', [
    h('label', { attrs: { for: 'voice-timer' } }, 'Timer'),
    h('input#voice-timer', {
      attrs: { type: 'range', min: 0, max: 5, step: 1 },
      hook: rangeConfig(timer, val => {
        timer(val);
        redraw();
      }),
    }),
    h('div.range_value', ['Off', '1.5s', '2s', '2.5s', '3s', '5s'][timer()]),
  ]);
}
