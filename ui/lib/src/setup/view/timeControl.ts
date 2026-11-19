import type { Prop } from '@/index';
import { hl, onChange, type MaybeVNode, type VNode } from '@/view';

import type { InputValue } from '../interfaces';
import { option } from '../option';
import {
  sliderTimes,
  sliderInitVal,
  timeVToTime,
  incrementVToIncrement,
  daysVToDays,
  type TimeControl,
  timeModes,
} from '../timeControl';

const showTime = (v: number) => {
  if (v === 1 / 4) return '¼';
  if (v === 1 / 2) return '½';
  if (v === 3 / 4) return '¾';
  return v.toString();
};

const blindModeTimePickers = (tc: TimeControl) => {
  return [
    renderTimeModePicker(tc),
    tc.mode() === 'realTime' &&
      hl('div.time-choice', [
        hl('label', { attrs: { for: 'sf_time' } }, i18n.site.minutesPerSide),
        hl(
          'select#sf_time',
          { on: onChange(v => tc.timeV(parseFloat(v))) },
          sliderTimes.map((sliderTime, timeV) =>
            option({ key: timeV.toString(), name: showTime(sliderTime) }, tc.timeV().toString()),
          ),
        ),
      ]),
    tc.mode() === 'realTime' &&
      hl('div.increment-choice', [
        hl('label', { attrs: { for: 'sf_increment' } }, i18n.site.incrementInSeconds),
        hl(
          'select#sf_increment',
          { on: onChange(v => tc.incrementV(parseInt(v))) },
          // 31 because the range below goes from 0 to 30
          Array.from(Array(31).keys()).map(incrementV =>
            option(
              { key: incrementV.toString(), name: incrementVToIncrement(incrementV).toString() },
              tc.incrementV().toString(),
            ),
          ),
        ),
      ]),
    tc.mode() === 'correspondence' &&
      hl('div.days-choice', [
        hl('label', { attrs: { for: 'sf_days' } }, i18n.site.daysPerTurn),
        hl(
          'select#sf_days',
          { on: onChange(v => tc.daysV(parseInt(v))) },
          // 7 because the range below goes from 1 to 7
          Array.from(Array(7).keys()).map(daysV =>
            option(
              { key: (daysV + 1).toString(), name: daysVToDays(daysV + 1).toString() },
              tc.daysV().toString(),
            ),
          ),
        ),
      ]),
  ];
};

const renderTimeModePicker = (tc: TimeControl) =>
  tc.canSelectMode() &&
  hl('div.label-select', [
    hl('label', { attrs: { for: 'sf_timeMode' } }, i18n.site.timeControl),
    hl(
      'select#sf_timeMode',
      {
        attrs: { 'aria-label': i18n.site.timeControl },
        on: onChange(tc.mode),
      },
      timeModes
        .filter(m => tc.modes.includes(m))
        .map(timeMode => option({ key: timeMode, name: i18n.site[timeMode] }, tc.mode())),
    ),
  ]);

const inputRange = (
  min: number,
  max: number,
  prop: Prop<InputValue>,
  id: string,
  classes?: Record<string, boolean>,
) =>
  hl('input.range', {
    class: classes,
    attrs: { type: 'range', min, max, value: prop(), id },
    hook: {
      update: (_: VNode, vnode: VNode) => {
        const el = vnode.elm as HTMLInputElement;
        el.value = prop().toString();
      },
    },
    on: { input: (e: Event) => prop(parseFloat((e.target as HTMLInputElement).value)) },
  });

export const timePickerAndSliders = (tc: TimeControl, minimumTimeRequiredIfReal = 0): VNode => {
  if (site.blindMode) return hl('div.config-group', blindModeTimePickers(tc));

  const activeMode = tc.mode();
  const showTabs = tc.canSelectMode();

  const tabs = showTabs
    ? hl(
        'div.tabs-horiz',
        {
          attrs: { role: 'tablist' },
        },
        tc.modes.map(mode =>
          hl(
            'button',
            {
              attrs: {
                role: 'tab',
                tabindex: activeMode === mode ? 0 : -1,
                'aria-selected': activeMode === mode,
              },
              class: { active: activeMode === mode },
              on: {
                click: () => tc.mode(mode),
              },
            },
            i18n.site[mode],
          ),
        ),
      )
    : null;

  let panelContent: MaybeVNode = null;

  if (activeMode === 'realTime') {
    const [tcTime, tcIncrement] = [tc.time(), tc.increment()];
    panelContent = hl('div.time-panel', { attrs: { role: 'tabpanel' } }, [
      hl('div.sliders-grid', [
        hl('div.slider-container', [
          hl(
            'div.label-row',
            hl('label', { for: 'gameTime' }, [
              i18n.site.minutesPerSide,
              hl('span.val-box', showTime(tcTime)),
            ]),
          ),
          inputRange(0, 38, tc.timeV, 'gameTime', {
            failure: !tc.realTimeValid(minimumTimeRequiredIfReal),
          }),
        ]),
        hl('div.slider-separator', '+'),
        hl('div.slider-container', [
          hl(
            'div.label-row',
            hl('label', { for: 'increment' }, [
              hl('span.val-box', tcIncrement.toString()),
              i18n.site.incrementInSeconds,
            ]),
          ),
          inputRange(0, 30, tc.incrementV, 'increment', {
            failure: !tc.realTimeValid(minimumTimeRequiredIfReal),
          }),
        ]),
      ]),
      hl(
        'div.presets',
        tc.presets.slice(0, 10).map(({ lim, inc }) =>
          hl(
            'button.preset-btn',
            {
              class: {
                active: tcTime === lim && tcIncrement === inc,
              },
              on: {
                click: () => {
                  tc.timeV(sliderInitVal(lim, timeVToTime, 100, 9));
                  tc.incrementV(sliderInitVal(inc, incrementVToIncrement, 100, 0));
                },
              },
            },
            `${showTime(lim)}+${inc}`,
          ),
        ),
      ),
    ]);
  } else if (activeMode === 'correspondence') {
    panelContent = hl('div.time-panel', { attrs: { role: 'tabpanel' } }, [
      hl('div.slider-container.full-width', [
        hl(
          'div.label-row',
          hl('label', { for: 'correspondenceDays' }, [
            i18n.site.daysPerTurn,
            hl('span.val-box', tc.days().toString()),
          ]),
        ),
        inputRange(1, 7, tc.daysV, 'correspondenceDays'),
      ]),
    ]);
  } else if (activeMode === 'unlimited') {
    panelContent = hl('div.time-panel', { attrs: { role: 'tabpanel' } }, i18n.site.unlimitedDescription);
  }

  return hl('div.config-group.time-control-tabs', [tabs, panelContent]);
};
