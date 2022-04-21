import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';

export const ratingDifferenceSliders = (ctrl: LobbyController) => {
  if (!ctrl.data.me || ctrl.opts.blindMode) return null;

  const { trans, setupCtrl } = ctrl;
  return h(
    'div.optional_config',
    h('div.rating-range-config', [
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
            input: (e: Event) => setupCtrl.ratingMin(parseInt((e.target as HTMLInputElement).value)),
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
            input: (e: Event) => setupCtrl.ratingMax(parseInt((e.target as HTMLInputElement).value)),
          },
        }),
      ]),
    ])
  );
};
