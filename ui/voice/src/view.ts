import * as licon from 'common/licon';
import { onInsert, bind, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { snabDialog, Dialog } from 'common/dialog';
import { onClickAway } from 'common';
import { Entry, VoiceCtrl } from './interfaces';
import { supportedLangs } from './voice';

export function renderVoiceBar(ctrl: VoiceCtrl, redraw: () => void, cls?: string) {
  return h(`div#voice-bar${cls ? '.' + cls : ''}`, [
    h('div#voice-status-row', [
      h('button#microphone-button', {
        hook: onInsert(el => el.addEventListener('click', () => ctrl.toggle())),
      }),
      h('span#voice-status', {
        hook: onInsert(el => site.mic.setController(voiceBarUpdater(ctrl, el))),
      }),
      h('button#voice-help-button', {
        attrs: { 'data-icon': licon.InfoCircle, title: 'Voice help' },
        hook: bind('click', () => ctrl.showHelp(true), undefined, false),
      }),
      h('button#voice-settings-button', {
        attrs: { 'data-icon': licon.Gear, title: 'Voice settings' },
        class: { active: ctrl.showPrefs() },
        hook: bind('click', () => ctrl.showPrefs.toggle(), redraw, false),
      }),
    ]),
    ctrl.showPrefs() &&
      h('div#voice-settings', { hook: onInsert(onClickAway(() => ctrl.showPrefs(false))) }, [
        deviceSelector(ctrl, redraw),
        langSetting(ctrl),
        ...(ctrl.module()?.prefNodes(redraw) ?? []),
        pushTalkSetting(ctrl),
      ]),
    ctrl.showHelp() && renderHelpModal(ctrl),
  ]);
}

export function flash() {
  const div = $as<HTMLElement>('#voice-status-row');
  div.classList.add('flash');
  div.onanimationend = () => div.classList.remove('flash');
}

function voiceBarUpdater(ctrl: VoiceCtrl, el: HTMLElement) {
  const voiceBtn = $('button#microphone-button');

  return (txt: string, tpe: Voice.MsgType) => {
    const classes: [string, boolean][] = [];
    classes.push(['listening', site.mic.isListening]);
    classes.push(['busy', site.mic.isBusy]);
    classes.push(['push-to-talk', ctrl.pushTalk() && !site.mic.isListening && !site.mic.isBusy]);
    classes.map(([clz, has]) => (has ? voiceBtn.addClass(clz) : voiceBtn.removeClass(clz)));
    voiceBtn.attr('data-icon', site.mic.isBusy ? licon.Cancel : licon.Voice);

    if (tpe !== 'partial') el.innerText = txt;
  };
}

function pushTalkSetting(ctrl: VoiceCtrl) {
  return h('div.voice-setting', { attrs: { style: 'align-self: center' } }, [
    h('div.switch', { attrs: { title: 'Hold the shift key while speaking' } }, [
      h('input#wake-mode.cmn-toggle', {
        attrs: { type: 'checkbox', checked: ctrl.pushTalk() },
        hook: bind('change', e => ctrl.pushTalk((e.target as HTMLInputElement).checked)),
      }),
      h('label', { attrs: { for: 'wake-mode' } }),
    ]),
    h('label', { attrs: { for: 'wake-mode' } }, ['Push ', h('strong', 'shift'), ' key to talk']),
  ]);
}

function langSetting(ctrl: VoiceCtrl) {
  return (
    supportedLangs.length > 1 &&
    h('div.voice-setting', [
      h('label', { attrs: { for: 'voice-lang' } }, 'Language'),
      h(
        'select#voice-lang',
        {
          attrs: { name: 'lang' },
          hook: bind('change', e => ctrl.lang((e.target as HTMLSelectElement).value)),
        },
        [
          ...supportedLangs.map(l =>
            h(
              'option',
              { attrs: l[0] === ctrl.lang() ? { value: l[0], selected: '' } : { value: l[0] } },
              l[1],
            ),
          ),
        ],
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
  return h('div.voice-setting', [
    h('label', { attrs: { for: 'voice-mic' } }, 'Microphone'),
    h(
      'select#voice-mic',
      {
        hook: onInsert((el: HTMLSelectElement) => {
          el.addEventListener('change', () => ctrl.micId(el.value));
          site.mic.getMics().then(ds => {
            devices = ds.length ? ds : [nullMic];
            redraw();
          });
        }),
      },
      devices.map(d =>
        h(
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
    if (!dlg.open) dlg.showModal();
  };

  return snabDialog({
    class: 'help.voice-move-help',
    htmlUrl: `/help/voice/${ctrl.moduleId}`,
    css: [{ hashed: 'voice.move.help' }],
    onClose: () => ctrl.showHelp(false),
    onInsert: async dlg => {
      if (ctrl.showHelp() === 'list') {
        showMoveList(dlg);
        return;
      }
      const grammar =
        ctrl.moduleId === 'coords'
          ? []
          : await xhr.jsonSimple(site.asset.url(`compiled/grammar/${ctrl.moduleId}-${ctrl.lang()}.json`));

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
      dlg.showModal();
    },
  });
}
