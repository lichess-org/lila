import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { makeVoiceCtrl } from './voiceCtrl';
import { makeMoveHandler } from './moveCtrl';
import { MoveCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';
export const voiceCtrl = makeVoiceCtrl(); // available outside of moveCtrl
export { makeMoveCtrl } from './moveCtrl';

export function render(ctrl: MoveCtrl) {
  return h('div.input-move', [
    h('input', {
      attrs: {
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) => {
        ctrl.registerHandler(makeMoveHandler({ input, ctrl })!);
      }),
    }),
    ctrl.isFocused()
      ? h('em', 'Enter SAN (Nc3), ICCF (2133) or UCI (b1c3) moves, type ? to learn more')
      : h('strong', 'Press <enter> to focus'),
    renderVoice(ctrl),
    ctrl.helpModalOpen()
      ? snabModal({
          class: 'keyboard-move-help',
          content: [h('div.scrollable', spinner())],
          onClose: () => ctrl.helpModalOpen(false),
          onInsert: async ($wrap: Cash) => {
            const [, html] = await Promise.all([
              lichess.loadCssPath('keyboardMove.help'),
              xhr.text(xhr.url('/help/keyboard-move', {})),
            ]);
            $wrap.find('.scrollable').html(html);
          },
        })
      : null,
  ]);
}

function renderVoice(ctrl: MoveCtrl) {
  return h('div.voice-move', [
    h('div', ctrl.voice.status),
    h('div#voice-move-button', {
      class: { enabled: ctrl.voice.recording },
      attrs: {
        role: 'button',
        ...dataIcon(''),
      },
      hook: bind('click', _ => {
        if (!ctrl.voice.recording) {
          ctrl.voice.start().then(() => ctrl.root.redraw());
        } else {
          ctrl.voice.stop();
          ctrl.root.redraw();
          return;
        }
      }),
    }),
  ]);
}
