import { h } from 'snabbdom';
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

  const ratingLabels = () => {
    if (site.blindMode) return [];
    else
      return [
        h('span.rating-min', '-' + Math.abs(currentRatingMin)),
        '/',
        h('span.rating-max', '+' + currentRatingMax),
      ];
  };

  if (site.blindMode && isProvisional)
    return h(
      'div',
      { attrs: { tabindex: 0 } },
      i18n.site.ratingRangeIsDisabledBecauseYourRatingIsProvisional,
    );
  else
    return h(
      `div.rating-range-config.optional-config${disabled}`,
      {
        attrs: isProvisional
          ? { title: i18n.site.ratingRangeIsDisabledBecauseYourRatingIsProvisional }
          : undefined,
      },
      [
        i18n.site.ratingRange,
        h('div.rating-range', [
          h('input.range.rating-range__min', {
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
          ...ratingLabels(),
          h('input.range.rating-range__max', {
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
