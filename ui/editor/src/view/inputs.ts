import { makeCsaHeader, parseCsaHeader } from 'shogiops/notation/csa/csa';
import { makeKifHeader, parseKifHeader } from 'shogiops/notation/kif/kif';
import { makeSfen, parseSfen } from 'shogiops/sfen';
import { VNode, h } from 'snabbdom';
import EditorCtrl from '../ctrl';

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
            el.setCustomValidity(valid ? '' : ctrl.trans.noarg('invalidSfen'));
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
              const kif = $('.copyables .kif textarea').val(),
                parsed = parseKifHeader(kif);
              if (parsed.isOk) ctrl.setSfen(makeSfen(parsed.value));
              ctrl.redraw();
            },
          },
        },
        ctrl.trans.noarg('importKif')
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
                  const csa = $('.copyables .csa textarea').val(),
                    parsed = parseCsaHeader(csa);
                  if (parsed.isOk) ctrl.setSfen(makeSfen(parsed.value));
                  ctrl.redraw();
                },
              },
            },
            ctrl.trans.noarg('importCsa')
          ),
        ])
      : null,
  ]);
}
