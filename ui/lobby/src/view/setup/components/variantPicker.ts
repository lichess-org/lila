import { hl, bind } from 'lib/view';
import type LobbyController from '@/ctrl';
import { variants, variantsForGameType } from '@/options';
import { option } from 'lib/setup/option';
import type { VNode } from 'snabbdom';

export const variantPicker = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;

  if (site.blindMode) {
    return hl('div.variant.label-select', [
      hl('label', { attrs: { for: 'sf_variant' } }, i18n.site.variant),
      hl(
        'select#sf_variant',
        {
          on: {
            change: (e: Event) => setupCtrl.variant((e.target as HTMLSelectElement).value as VariantKey),
          },
        },
        variantsForGameType(variants, setupCtrl.gameType!).map(variant =>
          option(variant, setupCtrl.variant()),
        ),
      ),
    ]);
  }

  const currentVariant = variants.find(v => v.key === setupCtrl.variant()) || variants[0];
  const isOpen = setupCtrl.variantMenuOpen();
  const toggleAction = () => {
    setupCtrl.toggleVariantMenu();
    setupCtrl.root.redraw();
  };

  const children: (VNode | string | undefined | null)[] = [
    hl(
      'label.mselect__label',
      {
        hook: bind('click', toggleAction),
      },
      [
        hl('span.icon', { attrs: { 'data-icon': currentVariant.icon } }),
        hl('div.text', [hl('span.name', currentVariant.name), hl('span.desc', currentVariant.description)]),
      ],
    ),
  ];

  if (isOpen) {
    children.push(hl('label.fullscreen-mask', { hook: bind('click', toggleAction) }));
    children.push(
      hl(
        'div.mselect__list',
        hl(
          'table',
          hl(
            'tbody',
            variantsForGameType(variants, setupCtrl.gameType!).map(v =>
              hl(
                'tr.mselect__item',
                {
                  class: { current: v.key === setupCtrl.variant() },
                  hook: bind('click', () => {
                    setupCtrl.variant(v.key);
                    setupCtrl.toggleVariantMenu();
                    setupCtrl.root.redraw();
                  }),
                },
                [
                  hl('td.icon', hl('span', { attrs: { 'data-icon': v.icon } })),
                  hl('td.name', v.name),
                  hl('td.desc', v.description),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  return hl(
    'div.mselect',
    {
      class: { mselect__active: isOpen },
    },
    children,
  );
};
