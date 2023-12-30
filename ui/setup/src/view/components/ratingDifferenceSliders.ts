import { h } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';

export const ratingDifferenceSliders = (ctrl: SetupCtrl) => {
  if (!ctrl.root.user || lichess.blindMode || !ctrl.root.ratingMap) return null;

  const selectedPerf = ctrl.selectedPerf();
  const isProvisional = !!ctrl.root.ratingMap[selectedPerf].prov;
  const disabled = isProvisional ? '.disabled' : '';

  // Get current rating values or use default values if isProvisional
  const currentRatingMin = isProvisional ? -500 : ctrl.ratingMin();
  const currentRatingMax = isProvisional ? 500 : ctrl.ratingMax();

  return h(
    `div.rating-range-config.optional-config${disabled}`,
    {
      attrs: isProvisional
        ? { title: 'Your rating is still provisional, play some rated games to use the rating range.' }
        : undefined,
    },
    [
      ctrl.root.trans('ratingRange'),
      h('div.rating-range', [
        h('input.range.rating-range__min', {
          attrs: {
            type: 'range',
            min: '-500',
            max: '0',
            step: '50',
            value: currentRatingMin,
            disabled: isProvisional,
          },
          on: {
            input: (e: Event) => {
              const newVal = parseInt((e.target as HTMLInputElement).value);
              if (newVal === 0 && ctrl.ratingMax() === 0) ctrl.ratingMax(50);
              ctrl.ratingMin(newVal);
            },
          },
        }),
        h('span.rating-min', '-' + Math.abs(currentRatingMin)),
        '/',
        h('span.rating-max', '+' + currentRatingMax),
        h('input.range.rating-range__max', {
          attrs: {
            type: 'range',
            min: '0',
            max: '500',
            step: '50',
            value: currentRatingMax,
            disabled: isProvisional,
          },
          on: {
            input: (e: Event) => {
              const newVal = parseInt((e.target as HTMLInputElement).value);
              if (newVal === 0 && ctrl.ratingMin() === 0) ctrl.ratingMin(-50);
              ctrl.ratingMax(newVal);
            },
          },
        }),
      ]),
    ],
  );
};
