import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { MoveCtrl } from '../interfaces';
import { makeKeyboardHandler } from '../keyboardMoveHandler';
import { helpModal } from './helpModal';

export function renderKeyboardView(ctrl: MoveCtrl) {
  return h('div.input-move', [
    h('input#keyboard-input', {
      attrs: {
        placeholder: ctrl.isFocused() ? 'Type ? for help' : 'Press enter to focus',
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) => ctrl.addHandler(makeKeyboardHandler({ input, ctrl }))),
    }),
    ctrl.helpModalOpen() ? helpModal(ctrl) : null,
  ]);
}
