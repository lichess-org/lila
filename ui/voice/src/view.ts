import { onClickAway } from 'lib';
import { licon } from 'lib/licon';
import { onInsert, bind, hl, type VNode, domDialog, dataIcon } from 'lib/view';
import { cmnToggleProp } from 'lib/view/cmn-toggle';
import { jsonSimple } from 'lib/xhr';

import type { Entry, VoiceCtrl, MsgType } from './interfaces';
import { supportedLangs } from './languages';

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
        attrs: { ...dataIcon(licon.InfoCircle), title: i18n.voiceCommands.voiceHelp },
        hook: bind('click', () => openHelpDialog(ctrl), undefined, false),
      }),
      hl('button#voice-settings-button', {
        attrs: { ...dataIcon(licon.Gear), title: i18n.voiceCommands.voiceSettings },
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
  ]);
}

function voiceBarUpdater(ctrl: VoiceCtrl, el: HTMLElement) {
  const voiceBtn = $('button#microphone-button');
  return (txt: string, tpe: MsgType) => {
    voiceBtn.toggleClass('listening', ctrl.mic.isListening);
    voiceBtn.toggleClass('error', tpe === 'error');
    voiceBtn.toggleClass('push-to-talk', ctrl.pushTalk() && !ctrl.mic.isListening && !ctrl.mic.isBusy);
    voiceBtn.html(ctrl.mic.isBusy ? '<span class="ddloader"></span>' : '');

    if (ctrl.mic.isBusy) voiceBtn.removeAttr('data-icon');
    else voiceBtn.attr('data-icon', tpe === 'error' ? licon.Cancel : licon.Voice);

    if (tpe !== 'partial') el.innerText = txt;
  };
}

function pushTalkSetting(ctrl: VoiceCtrl) {
  return hl('div.voice-setting', [
    hl('label.cmn-toggle-wrap', { attrs: { title: i18n.voiceCommands.holdShiftKeyWhileSpeaking } }, [
      cmnToggleProp({ id: 'wake-mode', prop: ctrl.pushTalk }),
      ...i18n.voiceCommands.pushShiftKeyToTalk.asArray(hl('kbd', 'Shift')),
    ]),
  ]);
}

function langSetting(ctrl: VoiceCtrl) {
  return (
    supportedLangs.length > 1 &&
    hl('div.voice-setting', [
      hl('label', { attrs: { for: 'voice-lang' } }, i18n.site.language),
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
    hl('label', { attrs: { for: 'voice-mic' } }, i18n.voiceCommands.microphone),
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
          d.deviceId === 'null' ? i18n.voiceCommands.noneSelected : d.label,
        ),
      ),
    ),
  ]);
}

export async function openHelpDialog(ctrl: VoiceCtrl): Promise<void> {
  const [dlg, { entries }] = await Promise.all([
    domDialog({
      closeButton: true,
      class: 'help.voice-move-help',
      htmlUrl: `/help/voice/${ctrl.moduleId}`,
      css: [{ key: 'voice.move.help' }],
      modal: true,
      easyClose: 'clickOutside',
      actions: [{ selector: '.all-phrases-button', listener: () => openAllPhrasesDialog(ctrl) }],
    }),
    ctrl.moduleId === 'coords'
      ? { entries: [] }
      : jsonSimple(site.asset.url(`compiled/grammar/${ctrl.moduleId}-${ctrl.lang()}.json`)).catch(() => ({
          entries: [],
        })),
  ]);
  ctrl.helpDialog?.close();
  ctrl.helpDialog = dlg;
  const valToWord = (val: string, phonetic: boolean) =>
    entries.find(
      (entry: Entry) => (entry.val ?? entry.tok) === val && (!phonetic || entry.tags?.includes('phonetic')),
    )?.in;
  dlg.view.querySelectorAll<HTMLElement>('.val-to-word').forEach(el => {
    const tryPhonetic = (val: string) =>
      (el.classList.contains('phonetic') && valToWord(val, true)) || valToWord(val, false);
    el.innerText = el.innerText
      .split(',')
      .map(v => tryPhonetic(v))
      .join(' ');
  });
  dlg.show();
}

export async function openAllPhrasesDialog(ctrl: VoiceCtrl): Promise<void> {
  const all =
    ctrl
      .module()
      ?.allPhrases()
      ?.sort((a, b) => a[0].localeCompare(b[0])) ?? [];
  const colCount = Math.min(3, Math.ceil(window.innerWidth / 399));
  const rowCount = Math.ceil(all.length / colCount);
  const rows: HTMLTableRowElement[] = [];
  for (let row = 0; row < rowCount; row++) {
    rows.push(document.createElement('tr'));
    for (let index = row; index < all.length; index += rowCount) {
      rows[row].insertCell().textContent = all[index][0];
      rows[row].insertCell().textContent = all[index][1];
    }
  }
  ctrl.helpDialog?.close();
  ctrl.helpDialog = await domDialog({
    closeButton: true,
    class: 'help.voice-move-help',
    htmlText: '<table class="big-table"><tbody></tbody></table>',
    insert: [{ nodes: rows, selector: 'tbody' }],
    css: [{ key: 'voice.move.help' }],
    modal: true,
    easyClose: 'clickOutside',
  });
  ctrl.helpDialog.show();
}
