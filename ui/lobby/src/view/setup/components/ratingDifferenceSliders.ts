import { hl } from 'lib/view';
import type LobbyController from '@/ctrl';

export const ratingDifferenceSliders = (ctrl: LobbyController) => {
  if (!ctrl.me || !ctrl.data.ratingMap) return null;

  const { setupCtrl } = ctrl;
  const isProvisional = setupCtrl.isProvisional();

  // Get current rating values or use default values if isProvisional
  const currentRatingMin = isProvisional ? -500 : setupCtrl.ratingMin();
  const currentRatingMax = isProvisional ? 500 : setupCtrl.ratingMax();

  const ratingInput = (type: 'min' | 'max') => {
    const isMin = type === 'min';
    return hl(`input.range.rating-range__${type}`, {
      attrs: {
        type: 'range',
        'aria-label': i18n.site.ratingFilter,
        min: isMin ? '-500' : '0',
        max: isMin ? '0' : '500',
        step: '50',
        value: isMin ? currentRatingMin : currentRatingMax,
        disabled: isProvisional,
      },
      on: {
        input: (e: Event) => {
          const newVal = parseInt((e.target as HTMLInputElement).value);
          // Both values should not be 0. Modify the other slider so there is always a range
          if (newVal === 0 && (isMin ? setupCtrl.ratingMax() : setupCtrl.ratingMin()) === 0)
            isMin ? setupCtrl.ratingMax(50) : setupCtrl.ratingMin(-50);
          isMin ? setupCtrl.ratingMin(newVal) : setupCtrl.ratingMax(newVal);
        },
      },
    });
  };

  return hl(
    'div',
    {
      class: { disabled: isProvisional },
      attrs: isProvisional
        ? {
            title: i18n.site.ratingRangeIsDisabledBecauseYourRatingIsProvisional,
            'aria-disabled': 'true',
            tabindex: 0,
          }
        : undefined,
    },
    [
      i18n.site.ratingFilter,
      hl('div.rating-range', [
        ratingInput('min'),
        !site.blindMode && [
          hl('span.rating-min', '-' + Math.abs(currentRatingMin)),
          '/',
          hl('span.rating-max', '+' + currentRatingMax),
        ],
        ratingInput('max'),
      ]),
    ],
  );
};
