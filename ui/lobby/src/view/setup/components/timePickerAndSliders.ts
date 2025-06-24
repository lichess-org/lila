import type { Prop } from 'lib';
import { hl } from 'lib/snabbdom';
import type LobbyController from '../../../ctrl';
import type { InputValue, TimeMode } from '../../../interfaces';
import { daysVToDays, incrementVToIncrement, sliderTimes, timeModes } from '../../../options';
import { option } from './option';

const showTime = (v: number) => {
  if (v === 1 / 4) return '¼';
  if (v === 1 / 2) return '½';
  if (v === 3 / 4) return '¾';
  return v.toString();
};

const renderBlindModeTimePickers = (ctrl: LobbyController, allowAnonymous: boolean) => {
  const { setupCtrl } = ctrl;
  return [
    renderTimeModePicker(ctrl, allowAnonymous),
    setupCtrl.timeMode() === 'realTime' &&
      hl('div.time-choice', [
        hl('label', { attrs: { for: 'sf_time' } }, i18n.site.minutesPerSide),
        hl(
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
      hl('div.increment-choice', [
        hl('label', { attrs: { for: 'sf_increment' } }, i18n.site.incrementInSeconds),
        hl(
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
      hl('div.days-choice', [
        hl('label', { attrs: { for: 'sf_days' } }, i18n.site.daysPerTurn),
        hl(
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
  const { setupCtrl } = ctrl;
  return (
    (ctrl.me || allowAnonymous) &&
    hl('div.label-select', [
      hl('label', { attrs: { for: 'sf_timeMode' } }, i18n.site.timeControl),
      hl(
        'select#sf_timeMode',
        {
          on: {
            change: (e: Event) => setupCtrl.timeMode((e.target as HTMLSelectElement).value as TimeMode),
          },
        },
        timeModes.map(timeMode => option(timeMode, setupCtrl.timeMode())),
      ),
    ])
  );
};

const inputRange = (min: number, max: number, prop: Prop<InputValue>, classes?: Record<string, boolean>) =>
  hl('input.range', {
    class: classes,
    attrs: { type: 'range', min, max, value: prop() },
    on: { input: (e: Event) => prop(parseFloat((e.target as HTMLInputElement).value)) },
  });

export const timePickerAndSliders = (ctrl: LobbyController, allowAnonymous = false) => {
  const { setupCtrl } = ctrl;
  return hl(
    'div.time-mode-config.optional-config',
    site.blindMode
      ? renderBlindModeTimePickers(ctrl, allowAnonymous)
      : [
          renderTimeModePicker(ctrl, allowAnonymous),
          setupCtrl.timeMode() === 'realTime' &&
            hl('div.time-choice.range', [
              `${i18n.site.minutesPerSide}: `,
              hl('span', showTime(setupCtrl.time())),
              inputRange(0, 38, setupCtrl.timeV, {
                failure: !setupCtrl.validTime() || !setupCtrl.validAiTime(),
              }),
            ]),
          setupCtrl.timeMode() === 'realTime'
            ? hl('div.increment-choice.range', [
                `${i18n.site.incrementInSeconds}: `,
                hl('span', `${setupCtrl.increment()}`),
                inputRange(0, 30, setupCtrl.incrementV, { failure: !setupCtrl.validTime() }),
              ])
            : setupCtrl.timeMode() === 'correspondence' &&
              hl(
                'div.correspondence',
                hl('div.days-choice.range', [
                  `${i18n.site.daysPerTurn}: `,
                  hl('span', `${setupCtrl.days()}`),
                  inputRange(1, 7, setupCtrl.daysV),
                ]),
              ),
        ],
  );
};
