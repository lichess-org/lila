import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import * as xhr from 'common/xhr';
import { MoveCtrl } from '../interfaces';
import { makeKeyboardHandler } from '../keyboardMoveHandler';

export function renderKeyboardView(ctrl: MoveCtrl) {
  return h('div.keyboard-move', [
    h('input#keyboard-input', {
      attrs: {
        placeholder: ctrl.isFocused() ? 'Type ? for help' : 'Press enter to focus',
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) => ctrl.addHandler(makeKeyboardHandler({ input, ctrl }))),
    }),
    ctrl.modalOpen() ? helpModal(ctrl) : null,
  ]);
}

export function helpModal(ctrl: MoveCtrl) {
  return snabModal({
    class: `keyboard-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.modalOpen(false),
    onInsert: async $ => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('inputMove.help'),
        xhr.text(xhr.url(`/help/keyboard-move`, {})),
      ]);
      $.find('.scrollable').html(html);
    },
  });
}
