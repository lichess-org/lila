import { Prop } from 'common';
import { looseH as h } from 'common/snabbdom';
import { SetupCtrl } from '../../ctrl';
import { InputValue, TimeMode } from '../../interfaces';
import { daysVToDays, incrementVToIncrement, sliderTimes, timeModes } from '../../options';
import { option } from './option';

const showTime = (v: number) => {
  if (v == 1 / 4) return '¼';
  if (v == 1 / 2) return '½';
  if (v == 3 / 4) return '¾';
  return v.toString();
};

const renderBlindModeTimePickers = (ctrl: SetupCtrl, allowAnonymous: boolean) => {
  return [
    renderTimeModePicker(ctrl, allowAnonymous),
    ctrl.timeMode() === 'realTime' &&
      h('div.time-choice', [
        h('label', { attrs: { for: 'sf_time' } }, ctrl.root.trans('minutesPerSide')),
        h(
          'select#sf_time',
          { on: { change: (e: Event) => ctrl.timeV(parseFloat((e.target as HTMLSelectElement).value)) } },
          sliderTimes.map((sliderTime, timeV) =>
            option({ key: timeV.toString(), name: showTime(sliderTime) }, ctrl.timeV().toString()),
          ),
        ),
      ]),
    ctrl.timeMode() === 'realTime' &&
      h('div.increment-choice', [
        h('label', { attrs: { for: 'sf_increment' } }, ctrl.root.trans('incrementInSeconds')),
        h(
          'select#sf_increment',
          { on: { change: (e: Event) => ctrl.incrementV(parseInt((e.target as HTMLSelectElement).value)) } },
          // 31 because the range below goes from 0 to 30
          Array.from(Array(31).keys()).map(incrementV =>
            option(
              { key: incrementV.toString(), name: incrementVToIncrement(incrementV).toString() },
              ctrl.incrementV().toString(),
            ),
          ),
        ),
      ]),
    ctrl.timeMode() === 'correspondence' &&
      h('div.days-choice', [
        h('label', { attrs: { for: 'sf_days' } }, ctrl.root.trans('daysPerTurn')),
        h(
          'select#sf_days',
          { on: { change: (e: Event) => ctrl.daysV(parseInt((e.target as HTMLSelectElement).value)) } },
          // 7 because the range below goes from 1 to 7
          Array.from(Array(7).keys()).map(daysV =>
            option(
              { key: (daysV + 1).toString(), name: daysVToDays(daysV + 1).toString() },
              ctrl.daysV().toString(),
            ),
          ),
        ),
      ]),
  ];
};

const renderTimeModePicker = (ctrl: SetupCtrl, allowAnonymous = false) => {
  const trans = ctrl.root.trans;
  return (
    (ctrl.root.user || allowAnonymous) &&
    h('div.label-select', [
      h('label', { attrs: { for: 'sf_timeMode' } }, trans('timeControl')),
      h(
        'select#sf_timeMode',
        { on: { change: (e: Event) => ctrl.timeMode((e.target as HTMLSelectElement).value as TimeMode) } },
        timeModes(trans).map(timeMode => option(timeMode, ctrl.timeMode())),
      ),
    ])
  );
};

const inputRange = (min: number, max: number, prop: Prop<InputValue>, classes?: Record<string, boolean>) =>
  h('input.range', {
    class: classes,
    attrs: { type: 'range', min, max, value: prop() },
    on: { input: (e: Event) => prop(parseFloat((e.target as HTMLInputElement).value)) },
  });

export const timePickerAndSliders = (ctrl: SetupCtrl, allowAnonymous = false) => {
  const trans = ctrl.root.trans;
  return h(
    'div.time-mode-config.optional-config',
    lichess.blindMode
      ? renderBlindModeTimePickers(ctrl, allowAnonymous)
      : [
          renderTimeModePicker(ctrl, allowAnonymous),
          ctrl.timeMode() === 'realTime' &&
            h('div.time-choice.range', [
              `${trans('minutesPerSide')}: `,
              h('span', showTime(ctrl.time())),
              inputRange(0, 38, ctrl.timeV, {
                failure: !ctrl.validTime() || !ctrl.validAiTime(),
              }),
            ]),
          ctrl.timeMode() === 'realTime'
            ? h('div.increment-choice.range', [
                `${trans('incrementInSeconds')}: `,
                h('span', `${ctrl.increment()}`),
                inputRange(0, 30, ctrl.incrementV, { failure: !ctrl.validTime() }),
              ])
            : ctrl.timeMode() === 'correspondence' &&
              h(
                'div.correspondence',
                h('div.days-choice.range', [
                  `${trans('daysPerTurn')}: `,
                  h('span', `${ctrl.days()}`),
                  inputRange(1, 7, ctrl.daysV),
                ]),
              ),
        ],
  );
};
