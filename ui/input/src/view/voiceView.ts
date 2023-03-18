import { MoveCtrl } from '../interfaces';
import { h, VNode, Hooks } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { storedBooleanProp } from 'common/storage';
import { voiceMoveCtrl } from '../voiceMoveCtrl';
import { voiceModal } from './voiceModal';

type ArrowPref = 'Colors' | 'Numbers';

export function renderVoiceView(ctrl: MoveCtrl, isPuzzle: boolean) {
  const rec = storedBooleanProp('recording', false);
  console.log('new one');
  return h(`div.voice-move${isPuzzle ? '.puz' : ''}`, [
    h('span#status-row', [
      h('a#voice-help-button', {
        attrs: { role: 'button', ...dataIcon('') },
        hook: bind('click', () => ctrl.modalOpen(true)),
      }),
      h('p#voice-status', {
        hook: onInsert(el => ctrl.voice.addListener('moveInput', txt => (el.innerText = txt))),
      }),
      h('a#microphone-button', {
        class: { enabled: ctrl.voice.isRecording, busy: ctrl.voice.isBusy },
        attrs: { role: 'button', ...dataIcon(ctrl.voice.isBusy ? '' : '') },
        hook: onInsert(el => {
          voiceMoveCtrl().registerMoveCtrl(ctrl);
          el.addEventListener('click', _ => {
            rec(!(ctrl.voice.isRecording || ctrl.voice.isBusy)) ? ctrl.voice.start() : ctrl.voice.stop();
          });
          if (rec() && !ctrl.voice.isRecording) setTimeout(() => el.dispatchEvent(new Event('click')));
        }),
      }),
      h('a#voice-settings-button', {
        attrs: { role: 'button', ...dataIcon('') },
        hook: bind('click', toggleSettings),
      }),
    ]),
    voiceSettings(ctrl),
    ctrl.modalOpen() ? voiceModal(ctrl) : null,
  ]);
}

const rangeConfig = (read: () => number, write: (value: number) => void): Hooks => ({
  insert: vnode => {
    const el = vnode.elm as HTMLInputElement;
    el.value = '' + read();
    el.addEventListener('input', _ => write(parseInt(el.value)));
    el.addEventListener('mouseout', _ => el.blur());
  },
});

function voiceSettings(ctrl: MoveCtrl): VNode {
  return h('div#voice-settings', { attrs: { style: 'display: none' } }, [
    h('div.setting', [
      h('label', { attrs: { for: 'conf' } }, 'Confidence'),
      h('input#conf', {
        attrs: {
          type: 'range',
          min: 0,
          max: 2,
          step: 1,
        },
        hook: rangeConfig(voiceMoveCtrl().arrogance, (val: number) => {
          voiceMoveCtrl().arrogance(val);
          ctrl.redraw();
        }),
      }),
      h('div.range_value', ['Mouse', 'Normal', 'Cowboy'][voiceMoveCtrl().arrogance()]),
    ]),

    h('div.choices', [
      'Label with',
      h(
        'span',
        ['Colors', 'Numbers'].map(x => choiceButton(x as ArrowPref))
      ),
    ]),
  ]);
}

function choiceButton(pref: ArrowPref) {
  const vmCtrl = voiceMoveCtrl();
  return h(
    `div#${pref}.choice.${vmCtrl.arrowColors() === (pref === 'Colors') ? 'selected' : ''}`,
    {
      hook: bind('click', () => {
        if (vmCtrl.arrowColors() !== vmCtrl.arrowColors(pref === 'Colors'))
          ['Colors', 'Numbers'].map(x => $(`#${x}`).toggleClass('selected'));
      }),
    },
    [h('label', pref)]
  );
}
function toggleSettings() {
  const settingsButton = $('#voice-settings-button');
  $('#voice-settings-button').toggleClass('active');
  if (settingsButton.hasClass('active')) {
    $('#voice-settings').show();
  } else {
    $('#voice-settings').hide();
  }
}
