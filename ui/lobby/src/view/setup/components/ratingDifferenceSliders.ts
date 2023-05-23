import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';

export const ratingDifferenceSliders = (ctrl: LobbyController) => {
  if (!ctrl.me || ctrl.opts.blindMode || !ctrl.data.ratingMap) return null;

  const { trans, setupCtrl } = ctrl;
  const selectedPerf = ctrl.setupCtrl.selectedPerf();
  const isProvisional = !!ctrl.data.ratingMap[selectedPerf].prov;
  const disabled = isProvisional ? '.disabled' : '';

  return h(
    `div.rating-range-config.optional-config${disabled}`,
    {
      attrs: isProvisional
        ? {
            title: 'Your rating is still provisional, play some rated games to use the rating range.',
          }
        : undefined,
    },
    [
      trans('ratingRange'),
      h('div.rating-range', [
        h('input.range.rating-range__min', {
          attrs: {
            type: 'range',
            min: '-500',
            max: '0',
            step: '50',
            value: setupCtrl.ratingMin(),
            disabled: isProvisional,
          },
          on: {
            input: (e: Event) => {
              const newVal = parseInt((e.target as HTMLInputElement).value);
              if (newVal === 0 && setupCtrl.ratingMax() === 0) setupCtrl.ratingMax(50);
              setupCtrl.ratingMin(newVal);
            },
          },
        }),
        h('span.rating-min', '-' + Math.abs(setupCtrl.ratingMin())),
        '/',
        h('span.rating-max', '+' + setupCtrl.ratingMax()),
        h('input.range.rating-range__max', {
          attrs: {
            type: 'range',
            min: '0',
            max: '500',
            step: '50',
            value: setupCtrl.ratingMax(),
            disabled: isProvisional,
          },
          on: {
            input: (e: Event) => {
              const newVal = parseInt((e.target as HTMLInputElement).value);
              if (newVal === 0 && setupCtrl.ratingMin() === 0) setupCtrl.ratingMin(-50);
              setupCtrl.ratingMax(newVal);
            },
          },
        }),
      ]),
    ]
  );
};
