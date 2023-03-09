import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { storedBooleanProp } from 'common/storage';
import { spinnerVdom as spinner } from 'common/spinner';
import { makeKeyboardHandler } from './keyboardMoveHandler';
import { voiceMoveCtrl } from './voiceMoveCtrl';
import { MoveCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';
export { makeMoveCtrl } from './moveCtrl';
export { voiceCtrl } from './voiceCtrl';

export function renderMoveCtrl(ctrl: MoveCtrl) {
  console.log(ctrl.root.keyboard);
  return ctrl.root.keyboard ? renderKeyboardMove(ctrl) : renderVoiceMove(ctrl);
}

function renderKeyboardMove(ctrl: MoveCtrl) {
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

function renderVoiceMove(ctrl: MoveCtrl) {
  /*document.addEventListener('contextmenu', e => {
    if ((e.target as Element)?.id === 'microphone-button') {
      showContextMenu(ctrl, e.target as HTMLElement);
      e.preventDefault();
    }
  });*/
  const rec = storedBooleanProp('recording', false);
  return h('div.input-move', [
    h('p#voice-status', {
      hook: onInsert(el => ctrl.voice.addListener('moveInput', txt => (el.innerText = txt))),
    }),
    h('a#voice-help-button', {
      attrs: { role: 'button', ...dataIcon('') },
      hook: bind('click', () => ctrl.helpModalOpen(true)),
    }),
    h('a#microphone-button', {
      class: { enabled: ctrl.voice.isRecording, busy: ctrl.voice.isBusy },
      attrs: { role: 'button', ...dataIcon(ctrl.voice.isBusy ? '' : '') },
      hook: onInsert(el => {
        voiceMoveCtrl().registerMoveCtrl(ctrl);
        el.addEventListener('click', _ => {
          rec(!(ctrl.voice.isRecording || ctrl.voice.isBusy)) ? ctrl.voice.start() : ctrl.voice.stop();
        });
        if (rec()) setTimeout(() => el.dispatchEvent(new Event('click')));
      }),
    }),
    ctrl.helpModalOpen() ? helpModal(ctrl) : null,
  ]);
}

/*
function showContextMenu(ctrl: MoveCtrl, micBtn: HTMLElement) {
  const el = document.createElement('div');
  el.addEventListener('mouseleave', () => micBtn.removeChild(el));
  el.id = 'microphone-context-menu';
  micBtn.appendChild(el);
  // never done the contextmenu event before, first let's see if it works in all browsers
  // & long press on mobile
  ctrl;
}*/

function helpModal(ctrl: MoveCtrl) {
  const tpe = ctrl.root.keyboard && !ctrl.voice.isRecording ? 'keyboard' : 'voice';
  return snabModal({
    class: `${tpe}-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.helpModalOpen(false),
    onInsert: async $ => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('inputMove.help'),
        xhr.text(xhr.url(`/help/${tpe}-move`, {})),
      ]);
      $.find('.scrollable').html(html);
    },
  });
}
