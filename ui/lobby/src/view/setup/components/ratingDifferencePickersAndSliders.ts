import { h, VNode } from 'snabbdom';
import LobbyController from '../../../ctrl';
import { option } from './option';

const renderRatingDifferencePickers = (ctrl: LobbyController, isProvisional: boolean): VNode => {
  const { trans, setupCtrl } = ctrl;

  // Get current rating values or use default values if isProvisional
  const currentRatingMinString = isProvisional ? "-500" : setupCtrl.ratingMin().toString();
  const currentRatingMaxString = isProvisional ? "500" : setupCtrl.ratingMax().toString();
  
  return h('div.rating-range-choice', [
    h('div.label-select', [
        h('label', { attrs: { for: 'sf_minRating' } }, trans('minimumRating')),
        h(
          'select#sf_minRating',
          {
            hook: {
              update: (_oldVnode: VNode, vnode: VNode) => {
                (vnode.elm as HTMLSelectElement).value = currentRatingMinString;
              },
            },
            attrs: {
              disabled: isProvisional,
            },
            on: {
              change: (e: Event): void => {
                const newVal = parseInt((e.target as HTMLSelectElement).value);
                if (newVal === 0 && currentRatingMaxString === "0") setupCtrl.ratingMax(50);
                setupCtrl.ratingMin(newVal);
              },
            },
          },
          Array.from({length: 11}, (_v, i) => {
            i *= -50;
            return option(
              { key: i.toString(), name: i.toString() },
              currentRatingMinString
            );
          })
        ),
      ]),
    h('div.label-select', [
        h('label', { attrs: { for: 'sf_maxRating' } }, trans('maximumRating')),
        h(
          'select#sf_maxRating',
          {
            hook: {
              update: (_oldVnode: VNode, vnode: VNode) => {
                (vnode.elm as HTMLSelectElement).value = currentRatingMaxString;
              },
            },
            attrs: {
              disabled: isProvisional,
            },
            on: {
              change: (e: Event): void => {
                const newVal = parseInt((e.target as HTMLSelectElement).value);
                if (newVal === 0 && currentRatingMinString === "0") setupCtrl.ratingMin(-50);
                setupCtrl.ratingMax(newVal);
              },
            },
          },
          Array.from({length: 11}, (_v, i) => {
            i *= 50;
            return option(
              { key: i.toString(), name: i.toString() },
              currentRatingMaxString
            );
          })
        ),
      ]),
    ]
  );
};

const renderRatingDifferenceSliders = (ctrl: LobbyController, isProvisional: boolean) => {
  const { setupCtrl } = ctrl;

  // Get current rating values or use default values if isProvisional
  const currentRatingMin = isProvisional ? -500 : setupCtrl.ratingMin();
  const currentRatingMax = isProvisional ? 500 : setupCtrl.ratingMax();

  return h('div.rating-range', [
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
          if (newVal === 0 && setupCtrl.ratingMax() === 0) setupCtrl.ratingMax(50);
          setupCtrl.ratingMin(newVal);
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
          if (newVal === 0 && setupCtrl.ratingMin() === 0) setupCtrl.ratingMin(-50);
          setupCtrl.ratingMax(newVal);
        },
      },
    }),
    ]
  )
}

export const ratingDifferencePickersAndSliders = (ctrl: LobbyController) => {
  if (!ctrl.me || !ctrl.data.ratingMap) return null;

  const { trans, setupCtrl } = ctrl;
  const selectedPerf = setupCtrl.selectedPerf();
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
      lichess.blindMode ? 
        renderRatingDifferencePickers(ctrl, isProvisional) : 
        renderRatingDifferenceSliders(ctrl, isProvisional)
    ]
  );
};
