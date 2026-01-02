import type { Prop } from '@/index';
import { hl, type VNode } from '@/view';
import type { InputValue } from '../interfaces';
import {
  timeModes,
  sliderTimes,
  sliderInitVal,
  timeVToTime,
  incrementVToIncrement,
  daysVToDays,
  type TimeControl,
  type TimeMode,
} from '../timeControl';
import { option } from '../option';

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
          {
            on: { change: (e: Event) => tc.timeV(parseFloat((e.target as HTMLSelectElement).value)) },
          },
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
          {
            on: {
              change: (e: Event) => tc.incrementV(parseInt((e.target as HTMLSelectElement).value)),
            },
          },
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
          {
            on: { change: (e: Event) => tc.daysV(parseInt((e.target as HTMLSelectElement).value)) },
          },
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
        on: {
          change: (e: Event) => {
            console.log('Time mode changed to', (e.target as HTMLSelectElement).value);
            tc.mode((e.target as HTMLSelectElement).value as TimeMode);
          },
        },
      },
      timeModes.filter(m => tc.modes.includes(m.key)).map(timeMode => option(timeMode, tc.mode())),
    ),
  ]);

const inputRange = (min: number, max: number, prop: Prop<InputValue>, classes?: Record<string, boolean>) =>
  hl('input.range', {
    class: classes,
    attrs: { type: 'range', min, max, value: prop() },
    hook: {
      update: (_: VNode, vnode: VNode) => {
        const el = vnode.elm as HTMLInputElement;
        el.value = prop().toString();
      },
    },
    on: { input: (e: Event) => prop(parseFloat((e.target as HTMLInputElement).value)) },
  });

export const timePickerAndSliders = (tc: TimeControl, minimumTimeRequiredIfReal: number = 0): VNode => {
  if (site.blindMode) return hl('div.config-group', blindModeTimePickers(tc));

  const activeMode = tc.mode();
  const showTabs = tc.canSelectMode();

  const tabs = showTabs
    ? hl(
        'div.tabs-horiz',
        tc.modes.map(mode =>
          hl(
            'span',
            {
              class: { active: activeMode === mode },
              on: { click: () => tc.mode(mode) },
            },
            timeModes.find(m => m.key === mode)?.name || mode,
          ),
        ),
      )
    : null;

  let panelContent: VNode | null = null;

  if (activeMode === 'realTime') {
    const [tcTime, tcIncrement] = [tc.time(), tc.increment()];
    panelContent = hl('div.time-panel', [
      hl('div.sliders-grid', [
        hl('div.slider-container', [
          hl('div.label-row', [hl('label', i18n.site.minutesPerSide), hl('span.val-box', showTime(tcTime))]),
          inputRange(0, 38, tc.timeV, {
            failure: !tc.realTimeValid(minimumTimeRequiredIfReal),
          }),
        ]),
        hl('div.slider-separator', '+'),
        hl('div.slider-container', [
          hl('div.label-row', [
            hl('span.val-box', tcIncrement.toString()),
            hl('label', i18n.site.incrementInSeconds),
          ]),
          inputRange(0, 30, tc.incrementV, { failure: !tc.realTimeValid(minimumTimeRequiredIfReal) }),
        ]),
      ]),
      hl(
        'div.presets',
        tc.presets.map(p =>
          hl(
            'button.preset-btn',
            {
              class: {
                active: tcTime === p.lim && tcIncrement === p.inc,
              },
              on: {
                click: () => {
                  tc.timeV(sliderInitVal(p.lim, timeVToTime, 100, 9));
                  tc.incrementV(sliderInitVal(p.inc, incrementVToIncrement, 100, 0));
                },
              },
            },
            `${showTime(p.lim)}+${p.inc}`,
          ),
        ),
      ),
    ]);
  } else if (activeMode === 'correspondence') {
    panelContent = hl('div.time-panel', [
      hl('div.slider-container.full-width', [
        hl('div.label-row', [hl('label', i18n.site.daysPerTurn), hl('span.val-box', tc.days().toString())]),
        inputRange(1, 7, tc.daysV),
      ]),
    ]);
  } else if (activeMode === 'unlimited') {
    panelContent = hl('div.time-panel', i18n.site.unlimitedDescription);
  }

  return hl('div.config-group.time-control-tabs', [tabs, panelContent]);
};
