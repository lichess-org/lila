import { h } from 'snabbdom';
import { snabDialog } from 'lib/view';
import type LobbyController from '@/ctrl';
import { variants, variantsForGameType } from '@/options';
import { option } from 'lib/setup/option';
import { Cogs } from 'lib/licon';

export const variantPicker = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;

  if (site.blindMode) {
    return h('div.variant.label-select', [
      h('label', { attrs: { for: 'sf_variant' } }, i18n.site.variant),
      h(
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

  const currentVariantKey = setupCtrl.variant();
  const isStandard = currentVariantKey === 'standard';
  const stdVariant = variants.find(v => v.key === 'standard')!;
  const activeVariant = !isStandard ? variants.find(v => v.key === currentVariantKey) : null;
  const otherDisplay = activeVariant
    ? { icon: activeVariant.icon, name: activeVariant.name, description: activeVariant.description }
    : { icon: Cogs, name: i18n.site.variants, description: i18n.site.variantsDescription };

  return h('div.variant-picker-split', [
    h('group.radio', [
      h('div', [
        h('input#setup-variant-std', {
          attrs: {
            type: 'radio',
            name: 'variant',
            value: 'standard',
            checked: isStandard,
          },
          props: {
            checked: isStandard,
          },
          // Force update the checked property to fix desync with native radio behavior
          hook: {
            update: (_: any, vnode: any) => {
              if (vnode.elm) vnode.elm.checked = isStandard;
            },
            insert: (vnode: any) => {
              if (vnode.elm) vnode.elm.checked = isStandard;
            },
          },
          on: { change: () => setupCtrl.variant('standard') },
        }),
        h('label', { attrs: { for: 'setup-variant-std' } }, [
          h('span.icon', { attrs: { 'data-icon': stdVariant.icon } }),
          h('div.text', [h('span.name', i18n.site.standard), h('span.desc', stdVariant.description)]),
        ]),
      ]),
      h('div', [
        h('input#setup-variant-other', {
          attrs: {
            type: 'radio',
            name: 'variant',
            value: 'other',
            checked: !isStandard,
          },
          props: {
            checked: !isStandard,
          },
          hook: {
            update: (_: any, vnode: any) => {
              if (vnode.elm) vnode.elm.checked = !isStandard;
            },
            insert: (vnode: any) => {
              if (vnode.elm) vnode.elm.checked = !isStandard;
            },
          },
        }),
        h(
          'label',
          {
            attrs: { for: 'setup-variant-other' },
            on: {
              click: (e: Event) => {
                e.preventDefault();
                setupCtrl.toggleVariantMenu();
              },
            },
          },
          [
            h('span.icon', { attrs: { 'data-icon': otherDisplay.icon } }),
            h('div.text', [h('span.name', otherDisplay.name), h('span.desc', otherDisplay.description)]),
          ],
        ),
      ]),
    ]),
  ]);
};

export const variantModal = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;

  if (!setupCtrl.variantMenuOpen()) return null;

  const currentVariant = setupCtrl.variant();
  const availableVariants = variantsForGameType(variants, setupCtrl.gameType!).filter(
    v => v.key !== 'standard',
  );

  const onClose = () => {
    setupCtrl.variantMenuOpen(false);
    setupCtrl.root.redraw();
  };

  let dialog: any;

  return snabDialog({
    attrs: { dialog: { 'aria-label': i18n.site.variants } },
    class: 'variant-selector',
    modal: true,
    onClose,
    vnodes: [
      h('h2', i18n.site.variants),
      h(
        'div.variant-grid',
        availableVariants.map(v => {
          return h(
            'button.variant-card',
            {
              class: { selected: currentVariant === v.key },
              on: {
                click: (e: Event) => {
                  e.stopPropagation();
                  setupCtrl.variant(v.key);
                  dialog?.close();
                },
              },
            },
            [
              h('span.icon', { attrs: { 'data-icon': v.icon } }),
              h('div.text', [h('span.name', v.name), h('span.desc', v.description)]),
            ],
          );
        }),
      ),
    ],
    onInsert: d => {
      dialog = d;
      d.show();
    },
  });
};
