import { hl } from 'lib/snabbdom';
import type LobbyController from '../../../ctrl';

export const ratingDifferenceSliders = (ctrl: LobbyController) => {
  if (!ctrl.me || !ctrl.data.ratingMap) return null;

  const { setupCtrl } = ctrl;
  const selectedPerf = ctrl.setupCtrl.selectedPerf();
  const isProvisional = !!ctrl.data.ratingMap[selectedPerf].prov;
  const disabled = isProvisional ? '.disabled' : '';

  // Get current rating values or use default values if isProvisional
  const currentRatingMin = isProvisional ? -500 : setupCtrl.ratingMin();
  const currentRatingMax = isProvisional ? 500 : setupCtrl.ratingMax();

  return hl(
    `div.rating-range-config.optional-config${disabled}`,
    {
      attrs: isProvisional
        ? {
            title: i18n.site.ratingRangeIsDisabledBecauseYourRatingIsProvisional,
            'aria-disabled': 'true',
            tabindex: 0,
          }
        : undefined,
    },
    [
      i18n.site.ratingRange,
      hl('div.rating-range', [
        hl('input.range.rating-range__min', {
          attrs: {
            type: 'range',
            'aria-label': i18n.site.ratingRange,
            min: '-500',
            max: '0',
            step: '50',
            value: currentRatingMin,
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
        !site.blindMode && [
          hl('span.rating-min', '-' + Math.abs(currentRatingMin)),
          '/',
          hl('span.rating-max', '+' + currentRatingMax),
        ],
        hl('input.range.rating-range__max', {
          attrs: {
            type: 'range',
            'aria-label': i18n.site.ratingRange,
            min: '0',
            max: '500',
            step: '50',
            value: currentRatingMax,
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
    ],
  );
};
