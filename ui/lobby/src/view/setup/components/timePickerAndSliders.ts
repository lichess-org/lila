import { Prop } from 'common';
import { looseH as h } from 'common/snabbdom';
import LobbyController from '../../../ctrl';
import { InputValue, TimeMode } from '../../../interfaces';
import { daysVToDays, incrementVToIncrement, sliderTimes, timeModes } from '../../../options';
import { option } from './option';

const showTime = (v: number) => {
  if (v == 1 / 4) return '¼';
  if (v == 1 / 2) return '½';
  if (v == 3 / 4) return '¾';
  return v.toString();
};

const renderBlindModeTimePickers = (ctrl: LobbyController, allowAnonymous: boolean) => {
  const { trans, setupCtrl } = ctrl;
  return [
    renderTimeModePicker(ctrl, allowAnonymous),
    setupCtrl.timeMode() === 'realTime' &&
      h('div.time-choice', [
        h('label', { attrs: { for: 'sf_time' } }, trans('minutesPerSide')),
        h(
          'select#sf_time',
          {
            on: { change: (e: Event) => setupCtrl.timeV(parseFloat((e.target as HTMLSelectElement).value)) },
          },
          sliderTimes.map((sliderTime, timeV) =>
            option({ key: timeV.toString(), name: showTime(sliderTime) }, setupCtrl.timeV().toString()),
          ),
        ),
      ]),
    setupCtrl.timeMode() === 'realTime' &&
      h('div.increment-choice', [
        h('label', { attrs: { for: 'sf_increment' } }, trans('incrementInSeconds')),
        h(
          'select#sf_increment',
          {
            on: {
              change: (e: Event) => setupCtrl.incrementV(parseInt((e.target as HTMLSelectElement).value)),
            },
          },
          // 31 because the range below goes from 0 to 30
          Array.from(Array(31).keys()).map(incrementV =>
            option(
              { key: incrementV.toString(), name: incrementVToIncrement(incrementV).toString() },
              setupCtrl.incrementV().toString(),
            ),
          ),
        ),
      ]),
    setupCtrl.timeMode() === 'correspondence' &&
      h('div.days-choice', [
        h('label', { attrs: { for: 'sf_days' } }, trans('daysPerTurn')),
        h(
          'select#sf_days',
          {
            on: { change: (e: Event) => setupCtrl.daysV(parseInt((e.target as HTMLSelectElement).value)) },
          },
          // 7 because the range below goes from 1 to 7
          Array.from(Array(7).keys()).map(daysV =>
            option(
              { key: (daysV + 1).toString(), name: daysVToDays(daysV + 1).toString() },
              setupCtrl.daysV().toString(),
            ),
          ),
        ),
      ]),
  ];
};

const renderTimeModePicker = (ctrl: LobbyController, allowAnonymous = false) => {
  const { trans, setupCtrl } = ctrl;
  return (
    (ctrl.me || allowAnonymous) &&
    h('div.label-select', [
      h('label', { attrs: { for: 'sf_timeMode' } }, trans('timeControl')),
      h(
        'select#sf_timeMode',
        {
          on: {
            change: (e: Event) => setupCtrl.timeMode((e.target as HTMLSelectElement).value as TimeMode),
          },
        },
        timeModes(trans).map(timeMode => option(timeMode, setupCtrl.timeMode())),
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

export const timePickerAndSliders = (ctrl: LobbyController, allowAnonymous = false) => {
  const { trans, setupCtrl } = ctrl;
  return h(
    'div.time-mode-config.optional-config',
    site.blindMode
      ? renderBlindModeTimePickers(ctrl, allowAnonymous)
      : [
          renderTimeModePicker(ctrl, allowAnonymous),
          setupCtrl.timeMode() === 'realTime' &&
            h('div.time-choice.range', [
              `${trans('minutesPerSide')}: `,
              h('span', showTime(setupCtrl.time())),
              inputRange(0, 38, setupCtrl.timeV, {
                failure: !setupCtrl.validTime() || !setupCtrl.validAiTime(),
              }),
            ]),
          setupCtrl.timeMode() === 'realTime'
            ? h('div.increment-choice.range', [
                `${trans('incrementInSeconds')}: `,
                h('span', `${setupCtrl.increment()}`),
                inputRange(0, 30, setupCtrl.incrementV, { failure: !setupCtrl.validTime() }),
              ])
            : setupCtrl.timeMode() === 'correspondence' &&
              h(
                'div.correspondence',
                h('div.days-choice.range', [
                  `${trans('daysPerTurn')}: `,
                  h('span', `${setupCtrl.days()}`),
                  inputRange(1, 7, setupCtrl.daysV),
                ]),
              ),
        ],
  );
};
