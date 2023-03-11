import { MoveCtrl } from '../interfaces';
import { h } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { storedBooleanProp } from 'common/storage';
import { voiceMoveCtrl } from '../voiceMoveCtrl';
import { helpModal } from './helpModal';

export function renderVoiceView(ctrl: MoveCtrl) {
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
