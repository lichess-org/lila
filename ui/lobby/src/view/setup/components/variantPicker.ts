import { hl } from 'lib/view';
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
  const inputId = 'mselect-variant';

  const toggleVariant = () => setupCtrl.toggleVariantMenu();
  const updateCheckboxAndToggle = () => {
    const checkbox = document.querySelector<HTMLInputElement>(`#${inputId}`);
    if (checkbox) checkbox.checked = false;
    toggleVariant();
  };

  const children: (VNode | string)[] = [
    hl('input.mselect__toggle', {
      attrs: { type: 'checkbox', id: inputId },
      on: { change: toggleVariant },
    }),
    hl(
      'label.mselect__label',
      {
        attrs: { for: inputId },
      },
      [
        hl('span.icon', { attrs: { 'data-icon': currentVariant.icon } }),
        hl('div.text', [hl('span.name', currentVariant.name), hl('span.desc', currentVariant.description)]),
      ],
    ),
  ];

  if (isOpen) {
    children.push(hl('label.fullscreen-mask', { on: { click: updateCheckboxAndToggle } }));
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
                  attrs: { tabindex: '0' },
                  on: {
                    click: () => {
                      setupCtrl.variant(v.key);
                      updateCheckboxAndToggle();
                    },
                    keydown: (event: KeyboardEvent) => {
                      if (event.key === 'Enter') {
                        setupCtrl.variant(v.key);
                        updateCheckboxAndToggle();
                      }
                    },
                  },
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
