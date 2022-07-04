import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';

export const ratingDifferenceSliders = (ctrl: LobbyController) => {
  if (!ctrl.me || ctrl.opts.blindMode) return null;

  const { trans, setupCtrl } = ctrl;
  return h('div.rating-range-config.optional-config', [
    trans('ratingRange'),
    h('div.rating-range', [
      h('input.range.rating-range__min', {
        attrs: {
          type: 'range',
          min: '-500',
          max: '0',
          step: '50',
          value: setupCtrl.ratingMin(),
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
  ]);
};
