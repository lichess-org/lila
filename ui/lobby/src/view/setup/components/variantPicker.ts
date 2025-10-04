import { onInsert } from 'lib/snabbdom';
import { h } from 'snabbdom';
import type LobbyController from '../../../ctrl';
import { variants, variantsForGameType } from '../../../options';
import { option } from 'lib/setup/option';

export const variantPicker = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  return h('div.variant.label-select', [
    h('label', { attrs: { for: 'sf_variant' } }, i18n.site.variant),
    h(
      'select#sf_variant',
      {
        on: { change: (e: Event) => setupCtrl.variant((e.target as HTMLSelectElement).value as VariantKey) },
        hook: onInsert<HTMLSelectElement>(element => element.focus()),
      },
      variantsForGameType(variants, setupCtrl.gameType!).map(variant => option(variant, setupCtrl.variant())),
    ),
  ]);
};
