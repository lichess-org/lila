import perfIcons from 'lib/game/perfIcons';
import { option } from 'lib/setup/option';
import { dataIcon, enter, hl } from 'lib/view';

import { variantsForGameType } from '@/options';
import type SetupController from '@/setupCtrl';

export const variantPicker = (setupCtrl: SetupController) => {
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
        variantsForGameType(setupCtrl.gameType!).map(variant =>
          option({ key: variant, name: i18n.variant[variant] }, setupCtrl.variant()),
        ),
      ),
    ]);
  }

  const currentVariant = setupCtrl.variant();
  const isOpen = setupCtrl.variantMenuOpen();
  const inputId = 'mselect-variant';

  const toggleVariant = () => setupCtrl.toggleVariantMenu();
  const updateCheckboxAndToggle = () => {
    const checkbox = document.querySelector<HTMLInputElement>(`#${inputId}`);
    if (checkbox) checkbox.checked = false;
    toggleVariant();
  };

  const children = [
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
        hl('span.icon', { attrs: dataIcon(perfIcons[currentVariant]) }),
        hl('div.text', [
          hl('span.name', i18n.variant[currentVariant]),
          hl('span.desc', i18n.variant[`${currentVariant}Title`]),
        ]),
      ],
    ),
  ];

  if (isOpen) {
    children.push(
      hl('div.fullscreen-mask', { on: { click: updateCheckboxAndToggle } }),
      hl(
        'div.mselect__list',
        hl(
          'table',
          hl(
            'tbody',
            variantsForGameType(setupCtrl.gameType!).map(v =>
              hl(
                'tr.mselect__item',
                {
                  class: { current: v === setupCtrl.variant() },
                  attrs: { tabindex: '0' },
                  on: {
                    click: () => {
                      setupCtrl.variant(v);
                      updateCheckboxAndToggle();
                    },
                    keydown: enter(() => {
                      setupCtrl.variant(v);
                      updateCheckboxAndToggle();
                    }),
                  },
                },
                [
                  hl('td.icon', hl('span', { attrs: dataIcon(perfIcons[v]) })),
                  hl('td.name', i18n.variant[v]),
                  hl('td.desc', i18n.variant[`${v}Title`]),
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
