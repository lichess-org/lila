import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert, dataIcon } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { makeKeyboardHandler } from './keyboardMoveHandler';
import { makeVoiceHandler } from './voiceMoveHandler';
import { MoveCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';
export { makeMoveCtrl } from './moveCtrl';
export { voiceCtrl } from './voiceCtrl';

export function renderMoveCtrl(ctrl: MoveCtrl) {
  return h('div.input-move', [
    h('a#voice-move-button', {
      class: { enabled: ctrl.voice.isRecording, busy: ctrl.voice.isBusy },
      attrs: {
        role: 'button',
        ...dataIcon(ctrl.voice.isBusy ? '\ue071' : ''),
      },
      hook: onInsert(el => {
        ctrl.addHandler(makeVoiceHandler(ctrl));
        el.addEventListener('click', _ =>
          ctrl.voice.isRecording || ctrl.voice.isBusy ? ctrl.voice.stop() : ctrl.voice.start()
        );
      }),
    }),
    h('input#move-text-input', {
      attrs: {
        placeholder: ctrl.voice.isRecording
          ? 'Listening...'
          : ctrl.isFocused()
          ? 'Type ? for help'
          : 'Press enter to focus',
        spellcheck: 'false',
        autocomplete: 'off',
        style: ctrl.root.keyboard ? '' : 'display: none;',
        disabled: ctrl.voice.isRecording,
      },
      hook: onInsert((input: HTMLInputElement) => {
        ctrl.addHandler(makeKeyboardHandler({ input, ctrl }));
        ctrl.voice.addListener('keyboardMoveInput', (text: string) => (input.value = text));
      }),
    }),
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
