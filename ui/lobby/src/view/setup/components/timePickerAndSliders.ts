import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';
import { TimeMode } from '../../../interfaces';
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
    setupCtrl.timeMode() === 'realTime'
      ? h('div.time_choice', [
          h('label', { attrs: { for: 'sf_time' } }, trans('minutesPerSide')),
          h(
            'select#sf_time',
            {
              on: {
                change: (e: Event) => setupCtrl.timeV(parseFloat((e.target as HTMLSelectElement).value)),
              },
            },
            sliderTimes.map((sliderTime, timeV) =>
              option({ key: timeV.toString(), name: showTime(sliderTime) }, setupCtrl.timeV().toString())
            )
          ),
        ])
      : null,
    setupCtrl.timeMode() === 'realTime'
      ? h('div.increment_choice', [
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
                setupCtrl.incrementV().toString()
              )
            )
          ),
        ])
      : null,
    setupCtrl.timeMode() === 'correspondence'
      ? h('div.days_choice', [
          h('label', { attrs: { for: 'sf_days' } }, trans('daysPerTurn')),
          h(
            'select#sf_days',
            {
              on: {
                change: (e: Event) => setupCtrl.daysV(parseInt((e.target as HTMLSelectElement).value)),
              },
            },
            // 7 because the range below goes from 1 to 7
            Array.from(Array(7).keys()).map(daysV =>
              option(
                { key: (daysV + 1).toString(), name: daysVToDays(daysV + 1).toString() },
                setupCtrl.daysV().toString()
              )
            )
          ),
        ])
      : null,
  ];
};

const renderTimeModePicker = (ctrl: LobbyController, allowAnonymous = false) => {
  const { trans, setupCtrl } = ctrl;
  const showTimeModePicker = ctrl.data.me || allowAnonymous;
  return showTimeModePicker
    ? h('div.label_select', [
        h('label', { attrs: { for: 'sf_timeMode' } }, trans('timeControl')),
        h(
          'select#sf_timeMode',
          {
            on: {
              change: (e: Event) => setupCtrl.timeMode((e.target as HTMLSelectElement).value as TimeMode),
            },
          },
          timeModes(trans).map(timeMode => option(timeMode, setupCtrl.timeMode()))
        ),
      ])
    : null;
};

export const timePickerAndSliders = (ctrl: LobbyController, allowAnonymous = false) => {
  const { trans, setupCtrl } = ctrl;
  return h(
    'div.time_mode_config.optional_config',
    ctrl.opts.blindMode
      ? renderBlindModeTimePickers(ctrl, allowAnonymous)
      : [
          renderTimeModePicker(ctrl, allowAnonymous),
          setupCtrl.timeMode() === 'realTime'
            ? h('div.time_choice.range', [
                `${trans('minutesPerSide')}: `,
                h('span', showTime(setupCtrl.time())),
                h('input.range', {
                  attrs: { type: 'range', min: '0', max: '38', value: setupCtrl.timeV() },
                  on: {
                    change: (e: Event) => setupCtrl.timeV(parseFloat((e.target as HTMLInputElement).value)),
                  },
                }),
              ])
            : null,
          setupCtrl.timeMode() === 'realTime'
            ? h('div.increment_choice.range', [
                `${trans('incrementInSeconds')}: `,
                h('span', setupCtrl.increment()),
                h('input.range', {
                  attrs: { type: 'range', min: '0', max: '30', value: setupCtrl.incrementV() },
                  on: {
                    change: (e: Event) => setupCtrl.incrementV(parseInt((e.target as HTMLInputElement).value)),
                  },
                }),
              ])
            : null,
          setupCtrl.timeMode() === 'correspondence'
            ? h(
                'div.correspondence',
                h('div.days_choice.range', [
                  `${trans('daysPerTurn')}: `,
                  h('span', setupCtrl.days()),
                  h('input.range', {
                    attrs: { type: 'range', min: '1', max: '7', value: setupCtrl.daysV() },
                    on: {
                      change: (e: Event) => setupCtrl.daysV(parseInt((e.target as HTMLInputElement).value)),
                    },
                  }),
                ])
              )
            : null,
        ]
  );
};
