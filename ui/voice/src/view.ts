import * as licon from 'lib/licon';
import { onInsert, bind, hl, type VNode, snabDialog, type Dialog, cmnToggleProp } from 'lib/view';
import { jsonSimple } from 'lib/xhr';
import { onClickAway } from 'lib';
import type { Entry, VoiceCtrl, MsgType } from './interfaces';
import { supportedLangs } from './voice';

export function renderVoiceBar(ctrl: VoiceCtrl, redraw: () => void, cls?: string): VNode {
  return hl(`div#voice-bar${cls ? '.' + cls : ''}`, [
    hl('div#voice-status-row', [
      hl('button#microphone-button', {
        hook: onInsert(el => el.addEventListener('click', () => ctrl.toggle())),
      }),
      hl('span#voice-status', {
        hook: onInsert(el => ctrl.mic.setController(voiceBarUpdater(ctrl, el))),
      }),
      hl('button#voice-help-button', {
        attrs: { 'data-icon': licon.InfoCircle, title: 'Voice help' },
        hook: bind('click', () => ctrl.showHelp(true), undefined, false),
      }),
      hl('button#voice-settings-button', {
        attrs: { 'data-icon': licon.Gear, title: 'Voice settings' },
        class: { active: ctrl.showPrefs() },
        hook: bind('click', () => ctrl.showPrefs.toggle(), redraw, false),
      }),
    ]),
    ctrl.showPrefs() &&
      hl('div#voice-settings', { hook: onInsert(onClickAway(() => ctrl.showPrefs(false))) }, [
        deviceSelector(ctrl, redraw),
        langSetting(ctrl),
        ctrl.module()?.prefNodes(redraw),
        pushTalkSetting(ctrl),
      ]),
    ctrl.showHelp() && renderHelpModal(ctrl),
  ]);
}

export function flash(): void {
  const div = document.querySelector<HTMLElement>('#voice-status-row')!;
  div.classList.add('flash');
  div.onanimationend = () => div.classList.remove('flash');
}

function voiceBarUpdater(ctrl: VoiceCtrl, el: HTMLElement) {
  const voiceBtn = $('button#microphone-button');
  return (txt: string, tpe: MsgType) => {
    voiceBtn.toggleClass('listening', ctrl.mic.isListening);
    voiceBtn.toggleClass('busy', ctrl.mic.isBusy);
    voiceBtn.toggleClass('push-to-talk', ctrl.pushTalk() && !ctrl.mic.isListening && !ctrl.mic.isBusy);
    voiceBtn.attr('data-icon', ctrl.mic.isBusy ? licon.Cancel : licon.Voice);

    if (tpe !== 'partial') el.innerText = txt;
  };
}

function pushTalkSetting(ctrl: VoiceCtrl) {
  return hl('div.voice-setting', [
    hl('label.cmn-toggle-wrap', { attrs: { title: 'Hold the shift key while speaking' } }, [
      cmnToggleProp({ id: 'wake-mode', prop: ctrl.pushTalk }),
      'Push ',
      hl('strong', 'shift'),
      ' key to talk',
    ]),
  ]);
}

function langSetting(ctrl: VoiceCtrl) {
  return (
    supportedLangs.length > 1 &&
    hl('div.voice-setting', [
      hl('label', { attrs: { for: 'voice-lang' } }, 'Language'),
      hl(
        'select#voice-lang',
        {
          attrs: { name: 'lang' },
          hook: bind('change', e => ctrl.lang((e.target as HTMLSelectElement).value)),
        },
        supportedLangs.map(l =>
          hl(
            'option',
            { attrs: l[0] === ctrl.lang() ? { value: l[0], selected: '' } : { value: l[0] } },
            l[1],
          ),
        ),
      ),
    ])
  );
}

const nullMic: MediaDeviceInfo = {
  deviceId: 'null',
  label: 'None selected',
  groupId: '',
  kind: 'audioinput',
  toJSON: () => '[]',
};

let devices: MediaDeviceInfo[] = [nullMic];
function deviceSelector(ctrl: VoiceCtrl, redraw: () => void) {
  return hl('div.voice-setting', [
    hl('label', { attrs: { for: 'voice-mic' } }, 'Microphone'),
    hl(
      'select#voice-mic',
      {
        hook: onInsert((el: HTMLSelectElement) => {
          el.addEventListener('change', () => ctrl.micId(el.value));
          ctrl.mic.getMics().then(ds => {
            devices = ds.length ? ds : [nullMic];
            redraw();
          });
        }),
      },
      devices.map(d =>
        hl(
          'option',
          {
            attrs: {
              value: d.deviceId,
              selected: d.deviceId === ctrl.micId(),
            },
          },
          d.label,
        ),
      ),
    ),
  ]);
}

function renderHelpModal(ctrl: VoiceCtrl) {
  const showMoveList = (dlg: Dialog) => {
    let html = '<table class="big-table"><tbody>';
    const all =
      ctrl
        .module()
        ?.allPhrases()
        ?.sort((a, b) => a[0].localeCompare(b[0])) ?? [];
    const cols = Math.min(3, Math.ceil(window.innerWidth / 399));
    const rows = Math.ceil(all.length / cols);
    for (let row = 0; row < rows; row++) {
      html += '<tr>';
      for (let i = row; i < all.length; i += rows) {
        html += `<td>${all[i][0]}</td><td>${all[i][1]}</td>`;
      }
      html += '</tr>';
    }
    html += '</tbody></table>';
    dlg.view.innerHTML = html;
    if (!dlg.dialog.open) dlg.show();
  };

  return snabDialog({
    class: 'help.voice-move-help',
    htmlUrl: `/help/voice/${ctrl.moduleId}`,
    css: [{ hashed: 'voice.move.help' }],
    onClose: () => ctrl.showHelp(false),
    modal: true,
    onInsert: async dlg => {
      if (ctrl.showHelp() === 'list') {
        showMoveList(dlg);
        return;
      }
      const grammar =
        ctrl.moduleId === 'coords'
          ? []
          : await jsonSimple(site.asset.url(`compiled/grammar/${ctrl.moduleId}-${ctrl.lang()}.json`)).catch(
              () => ({ entries: [] }),
            );

      const valToWord = (val: string, phonetic: boolean) =>
        grammar.entries.find(
          (e: Entry) => (e.val ?? e.tok) === val && (!phonetic || e.tags?.includes('phonetic')),
        )?.in;
      $('.val-to-word', dlg.view).each(function (this: HTMLElement) {
        const tryPhonetic = (val: string) =>
          (this.classList.contains('phonetic') && valToWord(val, true)) || valToWord(val, false);
        this.innerText = this.innerText
          .split(',')
          .map(v => tryPhonetic(v))
          .join(' ');
      });
      $('.all-phrases-button', dlg.view).on('click', () => showMoveList(dlg));
      dlg.show();
    },
  });
}
