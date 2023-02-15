import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { makeVoiceCtrl } from './voiceCtrl';
import { makeMoveHandler } from './moveCtrl';
import { MoveCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';
export { makeMoveCtrl } from './moveCtrl';

export const voiceCtrl = makeVoiceCtrl(); // available outside of moveCtrl

export function renderMoveCtrl(ctrl: MoveCtrl) {
  return h('div.input-move', [
    h('input', {
      attrs: { spellcheck: 'false', autocomplete: 'off', style: ctrl.root.keyboard ? '' : 'display: none;' },
      hook: onInsert((input: HTMLInputElement) => ctrl.registerHandler(makeMoveHandler({ input, ctrl })!)),
    }),
    ctrl.root.keyboard && !ctrl.voice.isRecording && !ctrl.voice.status
      ? ctrl.isFocused()
        ? h('strong', 'Type ? for help')
        : h('strong', 'Press enter to focus')
      : h('strong', ctrl.voice.status),
    h('div.voice-move', [
      ctrl.voice.isBusy ? spinner() : null,
      h('div#voice-move-button', {
        class: { enabled: ctrl.voice.isRecording },
        attrs: {
          role: 'button',
          ...dataIcon(''),
        },
        hook: bind('click', _ => {
          if (ctrl.voice.isRecording) ctrl.voice.stop();
          else ctrl.voice.start();
        }),
      }),
    ]),
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
