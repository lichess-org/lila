import { VNode, h } from 'snabbdom';
import { modal } from 'common/modal';
import { setup } from 'common/links';
import { EditorState } from '../interfaces';
import EditorCtrl from '../ctrl';
import { i18n } from 'i18n';

export function links(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.links', [analysis(ctrl, state), continueWith(ctrl, state), study(ctrl, state)]);
}

function analysis(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'a.button.text',
    {
      attrs: {
        'data-icon': 'A',
        rel: 'nofollow',
        ...(state.legalSfen
          ? { href: ctrl.makeAnalysisUrl(state.legalSfen, ctrl.bottomColor()) }
          : {}),
      },
      class: {
        disabled: !state.legalSfen,
      },
    },
    i18n('analysis')
  );
}

let openModal = false;
function continueWith(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'span.button.text',
    {
      attrs: { 'data-icon': 'U' },
      class: {
        disabled: !state.playable,
      },
      on: {
        click: () => {
          if (state.playable) {
            openModal = true;
            ctrl.redraw();
          }
        },
      },
    },
    [
      i18n('continueFromHere'),
      openModal
        ? modal({
            class: 'continue-with',
            onClose() {
              openModal = false;
              ctrl.redraw();
            },
            content: [
              h(
                'a.button.text',
                {
                  class: {
                    disabled: ['chushogi', 'annanshogi'].includes(ctrl.rules),
                  },
                  attrs: {
                    href: setup('/', ctrl.rules, state.legalSfen || '', 'ai'),
                    rel: 'nofollow',
                  },
                },
                i18n('playWithTheMachine')
              ),
              h(
                'a.button.text',
                {
                  attrs: {
                    href: setup('/', ctrl.rules, state.legalSfen || '', 'friend'),
                    rel: 'nofollow',
                  },
                },
                i18n('playWithAFriend')
              ),
            ],
          })
        : undefined,
    ]
  );
}

function study(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'form',
    {
      attrs: {
        method: 'post',
        action: '/study/as',
      },
    },
    [
      h('input', {
        attrs: {
          type: 'hidden',
          name: 'orientation',
          value: ctrl.bottomColor(),
        },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'variant', value: ctrl.rules },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'sfen', value: state.legalSfen || '' },
      }),
      h(
        'button.button.text',
        {
          attrs: {
            type: 'submit',
            'data-icon': '4',
            disabled: !state.legalSfen,
          },
          class: {
            disabled: !state.legalSfen,
          },
        },
        i18n('toStudy')
      ),
    ]
  );
}
