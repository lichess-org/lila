import { onInsert } from 'common/snabbdom';
import { h } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';
import { variantsBlindMode, variants, variantsForGameType } from '../../options';
import { option } from './option';

export const variantPicker = (ctrl: SetupCtrl) => {
  const baseVariants = lichess.blindMode ? variantsBlindMode : variants;
  return h('div.variant.label-select', [
    h('label', { attrs: { for: 'sf_variant' } }, ctrl.root.trans('variant')),
    h(
      'select#sf_variant',
      {
        on: {
          change: (e: Event) => ctrl.variant((e.target as HTMLSelectElement).value as VariantKey),
        },
        hook: onInsert<HTMLSelectElement>(element => element.focus()),
      },
      variantsForGameType(baseVariants, ctrl.gameType!).map(variant => option(variant, ctrl.variant())),
    ),
  ]);
};
