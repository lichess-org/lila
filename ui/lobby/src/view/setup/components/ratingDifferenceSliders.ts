import { hl } from 'lib/view';

import type LobbyController from '@/ctrl';

export const ratingDifferenceSliders = ({ setupCtrl, me, data }: LobbyController) => {
  const myRating = setupCtrl.myRating();

  if (!me || !data.ratingMap || !myRating) return null;

  const isProvisional = setupCtrl.isProvisional();

  const ratingInput = (type: 'min' | 'max') => {
    const isMin = type === 'min';
    return hl(`input.range.rating-range__${type}`, {
      attrs: {
        type: 'range',
        'aria-label':
          type === 'min'
            ? i18n.site.minRatingX(myRating + setupCtrl.ratingMin())
            : i18n.site.maxRatingX(myRating + setupCtrl.ratingMax()),
        min: isMin ? '-500' : '0',
        max: isMin ? '0' : '500',
        step: '50',
        disabled: isProvisional,
      },
      props: {
        value: isMin ? setupCtrl.ratingMin() : setupCtrl.ratingMax(),
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
    },
    isProvisional
      ? hl('span', i18n.site.ratingRangeIsDisabledBecauseYourRatingIsProvisional)
      : [
          i18n.site.ratingFilter,
          hl('div.rating-range', [
            ratingInput('min'),
            !site.blindMode && [
              hl('span.rating-min', '-' + Math.abs(setupCtrl.ratingMin())),
              '/',
              hl('span.rating-max', '+' + setupCtrl.ratingMax()),
            ],
            ratingInput('max'),
          ]),
        ],
  );
};
