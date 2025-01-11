import { i18n } from 'i18n';
import { makeCsaHeader, parseCsaHeader } from 'shogiops/notation/csa';
import { makeKifHeader, parseKifHeader } from 'shogiops/notation/kif';
import { makeSfen, parseSfen } from 'shogiops/sfen';
import { type VNode, h } from 'snabbdom';
import type EditorCtrl from '../ctrl';

export function inputs(ctrl: EditorCtrl, sfen: string): VNode | undefined {
  const pos = parseSfen(ctrl.rules, sfen);
  return h('div.copyables', [
    h('div.sfen', [
      h('strong', 'SFEN'),
      h('input.copyable', {
        attrs: {
          spellcheck: false,
        },
        props: {
          value: sfen,
        },
        on: {
          change(e) {
            const el = e.target as HTMLInputElement;
            ctrl.setSfen(el.value.trim());
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLInputElement;
            const valid = parseSfen(ctrl.rules, el.value.trim()).isOk;
            el.setCustomValidity(valid ? '' : i18n('invalidSfen'));
          },
          blur(e) {
            const el = e.target as HTMLInputElement;
            el.value = ctrl.getSfen();
            el.setCustomValidity('');
          },
        },
      }),
    ]),
    h('div.url', [
      h('strong.name', 'URL'),
      h('input.copyable.autoselect', {
        attrs: {
          readonly: true,
          spellcheck: false,
          value: ctrl.makeEditorUrl(sfen),
        },
      }),
    ]),
    h('div.kif', [
      h('strong', 'KIF'),
      h('textarea.copyable.autoselect', {
        attrs: {
          spellcheck: false,
        },
        props: {
          value: pos.isOk ? makeKifHeader(pos.value) : '',
        },
      }),
      h(
        'button.button.button-thin.action.text',
        {
          attrs: { 'data-icon': 'G' },
          on: {
            click: () => {
              const kifEl = document.querySelector(
                '.copyables .kif textarea',
              ) as HTMLTextAreaElement;
              const kif = kifEl.value;
              const parsed = parseKifHeader(kif);
              if (parsed.isOk) ctrl.setSfen(makeSfen(parsed.value));
              ctrl.redraw();
            },
          },
        },
        i18n('importKif'),
      ),
    ]),
    ctrl.rules === 'standard'
      ? h('div.csa', [
          h('strong', 'CSA'),
          h('textarea.copyable.autoselect', {
            attrs: {
              spellcheck: false,
            },
            props: {
              value: pos.isOk ? makeCsaHeader(pos.value) : '',
            },
          }),
          h(
            'button.button.button-thin.action.text',
            {
              attrs: { 'data-icon': 'G' },
              on: {
                click: () => {
                  const csaEl = document.querySelector(
                    '.copyables .csa textarea',
                  ) as HTMLTextAreaElement;
                  const csa = csaEl.value;
                  const parsed = parseCsaHeader(csa);
                  if (parsed.isOk) ctrl.setSfen(makeSfen(parsed.value));
                  ctrl.redraw();
                },
              },
            },
            i18n('importCsa'),
          ),
        ])
      : null,
  ]);
}
